#!/usr/bin/env python3
"""
Race test for the last copy of a book, across every lending-service instance.

1. The librarian creates a throwaway book with exactly 1 copy.
2. Every member fires borrow requests for it at the same moment; the load balancer spreads them
   over the lending-service instances.
3. Exactly one borrow may succeed, the copy count must end at 0, and every other request must be refused.
4. The test loan is cancelled and the book removed again (unless --keep).

Needs only Python 3 (no extra packages). Run it on the server, through the gateway:
    python3 race-test.py
or from a PC:
    python3 race-test.py --url http://<server-ip>
The password of the sample accounts is asked for (or read from BOOKNEST_PASSWORD).
"""
import argparse
import getpass
import json
import os
import random
import sys
import threading
import time
import urllib.error
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor


def call(base, method, path, token=None, body=None):
    """One HTTP call. Returns (status, json body or None, X-Lending-Instance header)."""
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(base + path, data=data, method=method)
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            text = response.read().decode()
            return response.status, (json.loads(text) if text else None), response.headers.get("X-Lending-Instance")
    except urllib.error.HTTPError as error:
        text = error.read().decode()
        try:
            parsed = json.loads(text) if text else None
        except ValueError:
            parsed = {"error": text[:80]}
        return error.code, parsed, error.headers.get("X-Lending-Instance")


def login(base, username, password):
    status, body, _ = call(base, "POST", "/auth/users/login", body={"username": username, "password": password})
    if status != 200:
        sys.exit(f"Login of {username} failed ({status}): {body}")
    return body["token"]


def unique_isbn():
    """A valid, random ISBN-13, so every run creates a new book."""
    digits = [9, 7, 8] + [random.randint(0, 9) for _ in range(9)]
    check = (10 - sum(d * (1 if i % 2 == 0 else 3) for i, d in enumerate(digits)) % 10) % 10
    return "".join(map(str, digits + [check]))


def main():
    parser = argparse.ArgumentParser(description="Race for the last copy of a book across all lending instances.")
    parser.add_argument("--url", default="http://localhost", help="the gateway address (default: http://localhost)")
    parser.add_argument("--librarian", default="librarian")
    parser.add_argument("--members", default="johnd,janed,alexm", help="comma-separated member usernames")
    parser.add_argument("--per-member", type=int, default=10, help="borrow requests each member sends at once")
    parser.add_argument("--keep", action="store_true", help="keep the test book and loan afterwards")
    args = parser.parse_args()

    base = args.url.rstrip("/")
    password = os.environ.get("BOOKNEST_PASSWORD") or getpass.getpass("Password of the sample accounts: ")
    members = [m.strip() for m in args.members.split(",") if m.strip()]

    librarian = login(base, args.librarian, password)
    tokens = {m: login(base, m, password) for m in members}

    # 1. a fresh book with exactly one copy
    _, authors, _ = call(base, "GET", "/api/authors", librarian)
    _, genres, _ = call(base, "GET", "/api/genres", librarian)
    title = "Race test " + time.strftime("%H:%M:%S")
    status, book, _ = call(base, "POST", "/api/books", librarian, {
        "title": title, "isbn": unique_isbn(), "publishedYear": 2026,
        "authorId": authors[0]["id"], "genreIds": [genres[0]["id"]], "totalCopies": 1})
    if status != 201:
        sys.exit(f"Could not create the test book ({status}): {book}")
    book_id = book["id"]
    print(f"Book {book_id} \"{title}\" created with 1 copy")

    # 2. every request waits at the barrier, then all of them are sent together
    jobs = [m for m in members for _ in range(args.per_member)]
    barrier = threading.Barrier(len(jobs))

    def borrow(member):
        barrier.wait()
        started = time.perf_counter()
        status, body, instance = call(base, "POST", "/api/loans", tokens[member], {"bookId": book_id, "duration": 7})
        return member, status, body, instance or "?", time.perf_counter() - started

    print(f"{len(jobs)} borrow requests from {len(members)} members, all at the same moment...")
    with ThreadPoolExecutor(max_workers=len(jobs)) as pool:
        results = list(pool.map(borrow, jobs))

    # 3. what happened
    winners = [r for r in results if r[1] == 201]
    outcomes = Counter("201 BORROWED" if r[1] == 201 else f"{r[1]} {(r[2] or {}).get('error', '')}" for r in results)
    per_instance = Counter(r[3] for r in results)

    print("\nOutcome of every request:")
    for outcome, count in outcomes.most_common():
        print(f"  {count:3}  {outcome}")
    print("\nRequests answered per lending instance:")
    for instance, count in sorted(per_instance.items()):
        print(f"  {count:3}  {instance}")
    for member, _, loan, instance, seconds in winners:
        print(f"\nWinner: {member}, loan {loan['id']}, answered by {instance} in {seconds:.2f} s")

    _, stock, _ = call(base, "GET", f"/api/inventory?bookIds={book_id}", librarian)
    available = stock[0]["availableCopies"] if stock else None
    print(f"Copies available afterwards: {available} (of 1)")

    checks = {
        "exactly one borrow succeeded": len(winners) == 1,
        "the copy count is 0, never below": available == 0,
        "every other request was refused with 409 (no errors)": all(r[1] == 409 for r in results if r[1] != 201),
        "more than one instance took part": len([i for i in per_instance if i != "?"]) > 1,
    }
    print()
    for check, ok in checks.items():
        print(f"  [{'PASS' if ok else 'FAIL'}] {check}")

    # 4. leave the library as it was
    if not args.keep:
        for _, _, loan, _, _ in winners:
            call(base, "PUT", f"/api/loans/{loan['id']}/cancel", librarian)
        status, _, _ = call(base, "DELETE", f"/api/books/{book_id}", librarian)
        print(f"\nCleanup: test loan cancelled, book {book_id} removed ({status})")

    sys.exit(0 if all(checks.values()) else 1)


if __name__ == "__main__":
    main()
