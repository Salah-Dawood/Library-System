import { API } from "/js/api.js";
import { h, replace, spinner, empty, field, input, select, toast, toastError, busy } from "/js/ui.js";

/* Two experiments: take the last copy, and break the five-loan limit.
   Each one can run without locking (shows the bug) or with locking (shows the fix). */

export async function racePage(ctx) {
  let books = [], members = [];
  try {
    const [bookPage, userPage] = await Promise.all([API.books({ size: 100, sortBy: "title", sortDir: "asc" }), API.users({ size: 100 })]);
    books = bookPage.content;
    members = userPage.content.filter((u) => u.role !== "librarian" && u.isActive);
  } catch (error) { return empty("Could not load the data for the lab", error.message); }

  return h("div", { class: "stack", style: "gap:22px" },
    h("p", { class: "muted", style: "max-width:72ch" },
      "Many threads act at the same moment. Run each experiment once without locking to see the bug, then with locking to see the fix. The lab needs the server to run with the race-demo profile."),
    h("div", { class: "lab" }, lastCopyLab(books), loanLimitLab(members)));
}

function labCard(title, text, controls, buttons, output) {
  return h("div", { class: "panel panel-pad stack" }, h("h2", null, title), h("p", { class: "muted" }, text), controls, buttons, output);
}

function resultCard(heading, ok, cells, verdict, strip) {
  return h("div", { class: "run-card" },
    h("h3", null, heading),
    strip,
    h("dl", { class: "kv" }, cells.flatMap(([k, v]) => [h("dt", null, k), h("dd", { class: "num" }, String(v))])),
    h("div", { class: "verdict " + (ok ? "ok" : "bad"), style: "margin-top:12px" }, verdict));
}

function strip(total, filled, limit) {
  const cells = [];
  for (let i = 0; i < total; i++) {
    let cls = "";
    if (i < filled) cls = limit != null && i >= limit ? "bad" : "won";
    cells.push(h("i", { class: cls }));
  }
  return h("div", { class: "threads-vis", "aria-hidden": "true" }, cells);
}

/* ---------- last copy ---------- */

function lastCopyLab(books) {
  const out = h("div", { class: "runs" });
  const pick = select("bookId", books.map((b) => ({ value: b.id, label: b.title })), books[0] ? books[0].id : "");
  const threads = input("threads", { type: "number", min: "2", max: "20", value: "10" });

  async function run(safe) {
    const r = await API.raceLastCopy(pick.value, Number(threads.value), safe);
    const ok = r.membersWhoGotACopy <= 1;
    return resultCard(safe ? "With locking" : "Without locking", ok, [
      ["Threads", r.threads], ["Copies at start", r.copiesAtStart], ["Members who got a copy", r.membersWhoGotACopy], ["Copies left afterwards", r.availableCopiesAfter],
    ], r.verdict, strip(r.threads, r.membersWhoGotACopy, 1));
  }

  async function go(event, modes) {
    await busy(event.currentTarget, async () => {
      replace(out, spinner());
      try {
        const cards = [];
        for (const safe of modes) cards.push(await run(safe));
        replace(out, ...cards);
      } catch (error) { replace(out); toastError(error); }
    });
  }

  return labCard("Take the last copy",
    "A book has one copy and many members try to borrow it at once. The book is set to one copy for the test and restored afterwards. Pick a book with no active loans.",
    h("div", { class: "form-grid" }, field("Book", pick), field("Threads", threads, "2 to 20")),
    h("div", { class: "row wrap" },
      h("button", { class: "btn danger", type: "button", onclick: (e) => go(e, [false]) }, "Run without locking"),
      h("button", { class: "btn", type: "button", onclick: (e) => go(e, [true]) }, "Run with locking"),
      h("button", { class: "btn secondary", type: "button", onclick: (e) => go(e, [false, true]) }, "Run both")),
    out);
}

/* ---------- loan limit ---------- */

function loanLimitLab(members) {
  const out = h("div", { class: "runs" });
  const pick = select("username", members.map((m) => ({ value: m.username, label: m.username })), members[0] ? members[0].username : "");
  const threads = input("threads", { type: "number", min: "6", max: "20", value: "8" });

  async function run(safe) {
    const r = await API.raceLoanLimit(pick.value, Number(threads.value), safe);
    const total = r.loansAfter;
    const ok = total <= r.limit;
    return resultCard(safe ? "With locking" : "Without locking", ok, [
      ["Threads", r.threads], ["Loan limit", r.limit], ["Loans before", r.loansBefore], ["Borrowed during the race", r.borrowedDuringRace], ["Loans after", r.loansAfter],
    ], r.verdict, strip(Math.max(r.threads, total), total, r.limit));
  }

  async function go(event, modes) {
    if (!pick.value) { toast("No active members to test with", "error"); return; }
    await busy(event.currentTarget, async () => {
      replace(out, spinner());
      try {
        const cards = [];
        for (const safe of modes) cards.push(await run(safe));
        replace(out, ...cards);
      } catch (error) { replace(out); toastError(error); }
    });
  }

  return labCard("Break the loan limit",
    "One member tries to borrow several different books at the same moment. The limit is five loans. You need as many books with a free copy as threads. Temporary loans are removed afterwards.",
    h("div", { class: "form-grid" }, field("Member", pick), field("Threads", threads, "6 to 20")),
    h("div", { class: "row wrap" },
      h("button", { class: "btn danger", type: "button", onclick: (e) => go(e, [false]) }, "Run without locking"),
      h("button", { class: "btn", type: "button", onclick: (e) => go(e, [true]) }, "Run with locking"),
      h("button", { class: "btn secondary", type: "button", onclick: (e) => go(e, [false, true]) }, "Run both")),
    out);
}
