import { API } from "/js/api.js";
import {
  h, replace, spinner, empty, field, input, select, table, pager, drawer, formModal, confirmBox,
  toast, toastError, busy, statusBadge, fmtDate, fmtDateTime, fmtMoney, timeLeft, formData,
} from "/js/ui.js";

function stat(label, value) {
  return h("div", { class: "stat" }, h("b", { class: "num" }, String(value)), h("span", null, label));
}

/* ---------- all loans ---------- */

export async function loansPage(ctx) {
  const state = { status: "", page: 0, sortBy: "createdAt", sortDir: "desc" };
  const list = h("div");
  const statusBox = select("status", [
    { value: "", label: "All loans" }, { value: "BORROWED", label: "On loan" },
    { value: "PENDING_REVIEW", label: "Waiting for review" }, { value: "RETURNED", label: "Returned" }, { value: "CANCELLED", label: "Cancelled" },
  ], "");
  const sortBox = select("sortBy", [
    { value: "createdAt", label: "Newest first" }, { value: "dueDate", label: "Due date" }, { value: "loanDate", label: "Borrow date" }, { value: "returnDate", label: "Return date" }, { value: "status", label: "Status" },
  ], "createdAt");
  const dirBox = select("sortDir", [{ value: "desc", label: "Descending" }, { value: "asc", label: "Ascending" }], "desc");
  const form = h("form", { class: "toolbar", onsubmit: (e) => { e.preventDefault(); state.status = statusBox.value; state.sortBy = sortBox.value; state.sortDir = dirBox.value; state.page = 0; load(); } },
    field("Status", statusBox), field("Sort by", sortBox), field("Order", dirBox), h("button", { class: "btn", type: "submit" }, "Apply"));

  async function load() {
    replace(list, spinner());
    try {
      const page = await API.allLoans({ status: state.status, page: state.page, size: 12, sortBy: state.sortBy, sortDir: state.sortDir });
      replace(list, h("div", { class: "panel" }, table([
        { label: "Book", render: (l) => h("a", { href: "#/books/" + l.bookId }, l.bookTitle) },
        { label: "Member", render: (l) => h("a", { href: "#", onclick: (e) => { e.preventDefault(); memberDrawer(l.userId); } }, l.username) },
        { label: "Borrowed", render: (l) => fmtDate(l.loanDate) },
        { label: "Due", render: (l) => h("span", null, fmtDate(l.dueDate), l.overdue ? h("span", { class: "badge red", style: "margin-left:8px" }, l.daysLate + " late") : null) },
        { label: "Status", render: (l) => statusBadge(l.status) },
        { label: "", class: "actions", render: (l) => l.status === "BORROWED" ? h("button", { class: "btn danger sm", type: "button", onclick: () => cancel(l) }, "Cancel loan") : l.status === "PENDING_REVIEW" ? h("a", { class: "btn secondary sm", href: "#/lib/returns" }, "Review") : null },
      ], page.content, empty("No loans match", "Change the status filter to see more.")), pager(page, (p) => { state.page = p; load(); })));
    } catch (error) { replace(list, empty("Could not load loans", error.message)); }
  }

  async function cancel(loan) {
    if (!(await confirmBox("Cancel this loan?", loan.username + " will no longer have \"" + loan.bookTitle + "\". The copy goes back on the shelf or to the next person waiting.", "Cancel loan", true))) return;
    try { await API.cancelLoan(loan.id); toast("Loan cancelled"); load(); } catch (error) { toastError(error); }
  }

  ctx.onLive(load);
  load();
  return h("div", null, form, list);
}

/* ---------- return reviews ---------- */

export async function returnsPage(ctx) {
  const state = { page: 0 };
  const list = h("div");
  async function load() {
    replace(list, spinner());
    try {
      const page = await API.returnsReview({ page: state.page, size: 10 });
      replace(list, h("div", { class: "panel" }, table([
        { label: "Book", render: (l) => h("a", { href: "#/books/" + l.bookId }, l.bookTitle) },
        { label: "Member", render: (l) => l.username },
        { label: "Due", render: (l) => fmtDate(l.dueDate) },
        { label: "Returned", render: (l) => fmtDate(l.returnDate) },
        { label: "Timing", render: (l) => l.returnTiming === "LATE" ? h("span", { class: "badge red" }, l.daysLate + " days late") : l.returnTiming === "EARLY" ? h("span", { class: "badge green" }, "Early") : h("span", { class: "badge green" }, "On time") },
        { label: "", class: "actions", render: (l) => h("button", { class: "btn sm", type: "button", onclick: () => review(l) }, "Review return") },
      ], page.content, empty("Nothing to review", "Returned books that need a check appear here.")), pager(page, (p) => { state.page = p; load(); })));
    } catch (error) { replace(list, empty("Could not load returns", error.message)); }
  }

  function review(loan) {
    const damaged = h("input", { type: "checkbox", name: "damaged" });
    const notes = h("textarea", { class: "input", name: "notes", maxlength: "500" });
    formModal("Review return", loan.bookTitle + ", returned by " + loan.username, [
      h("label", { class: "check" }, damaged, "The copy came back damaged"),
      field("Notes for the member", notes, "Optional, up to 500 characters"),
      field("Extra fine (optional)", input("fineAmount", { type: "number", min: "0", step: "0.01", placeholder: "0.00" }), "Late fees are added automatically"),
    ], "Complete review", async (data) => {
      await API.reviewReturn(loan.id, { damaged: damaged.checked, notes: data.notes, fineAmount: data.fineAmount === "" ? 0 : Number(data.fineAmount) });
      toast("Return reviewed");
      load();
    });
  }

  ctx.onLive(load);
  load();
  return list;
}

/* ---------- waitlists ---------- */

export async function waitlistPage(ctx) {
  const holder = h("div");
  const books = await API.books({ size: 100, sortBy: "title", sortDir: "asc" }).then((p) => p.content).catch(() => []);
  const pick = select("bookId", books.map((b) => ({ value: b.id, label: b.title + " (" + b.availableCopies + " free)" })), books[0] ? books[0].id : "");
  async function load() {
    if (!pick.value) { replace(holder, empty("No books yet", "Add books to the catalog first.")); return; }
    replace(holder, spinner());
    try {
      const rows = await API.bookQueue(pick.value);
      replace(holder, h("div", { class: "panel" }, table([
        { label: "Place", class: "num", render: (r) => r.status === "HOLD_READY" ? "Holding" : r.position },
        { label: "Member", render: (r) => r.username },
        { label: "Status", render: (r) => statusBadge(r.status) },
        { label: "Hold ends", render: (r) => r.status === "HOLD_READY" ? h("span", null, fmtDateTime(r.holdExpiresAt), " ", h("span", { class: "muted small" }, "(" + timeLeft(r.holdExpiresAt) + ")")) : "-" },
        { label: "Joined", render: (r) => fmtDateTime(r.joinedAt) },
      ], rows, empty("Nobody is waiting for this book"))));
    } catch (error) { replace(holder, empty("Could not load the waitlist", error.message)); }
  }
  pick.addEventListener("change", load);
  ctx.onLive(load);
  load();
  return h("div", null, h("div", { class: "toolbar" }, h("div", { class: "grow", style: "min-width:260px" }, field("Book", pick))), holder);
}

/* ---------- all fines ---------- */

export async function finesPage(ctx) {
  const holder = h("div");
  async function load() {
    replace(holder, spinner());
    try {
      const rows = await API.allFines();
      const owed = rows.filter((f) => f.status === "PENDING").reduce((sum, f) => sum + Number(f.amount), 0);
      replace(holder,
        h("div", { class: "stats" }, stat("Unpaid", fmtMoney(owed)), stat("Fines in total", rows.length)),
        h("div", { class: "panel" }, table([
          { label: "Fine", class: "num", render: (f) => "#" + f.id },
          { label: "Loan", class: "num", render: (f) => "#" + f.loanId },
          { label: "Reason", render: (f) => f.reason },
          { label: "Type", render: (f) => statusBadge(f.type) },
          { label: "Amount", class: "num", render: (f) => fmtMoney(f.amount) },
          { label: "Status", render: (f) => statusBadge(f.status) },
          { label: "", class: "actions", render: (f) => f.status === "PENDING" ? h("button", { class: "btn secondary sm", type: "button", onclick: () => pay(f) }, "Mark as paid") : null },
        ], rows, empty("No fines yet"))));
    } catch (error) { replace(holder, empty("Could not load fines", error.message)); }
  }
  async function pay(fine) {
    if (!(await confirmBox("Mark as paid?", fmtMoney(fine.amount) + " for " + fine.reason + ".", "Mark as paid"))) return;
    try { await API.payFine(fine.id); toast("Fine paid"); load(); } catch (error) { toastError(error); }
  }
  ctx.onLive(load);
  load();
  return holder;
}

/* ---------- members ---------- */

export async function usersPage(ctx) {
  const state = { page: 0, sortBy: "id", sortDir: "asc" };
  const list = h("div");
  const sortBox = select("sortBy", [
    { value: "id", label: "Id" }, { value: "username", label: "Username" }, { value: "emailAddress", label: "Email" }, { value: "createdAt", label: "Joined" },
  ], "id");
  const dirBox = select("sortDir", [{ value: "asc", label: "Ascending" }, { value: "desc", label: "Descending" }], "asc");
  const form = h("form", { class: "toolbar", onsubmit: (e) => { e.preventDefault(); state.sortBy = sortBox.value; state.sortDir = dirBox.value; state.page = 0; load(); } },
    field("Sort by", sortBox), field("Order", dirBox), h("button", { class: "btn", type: "submit" }, "Apply"));

  async function load() {
    replace(list, spinner());
    try {
      const page = await API.users({ page: state.page, size: 12, sortBy: state.sortBy, sortDir: state.sortDir });
      replace(list, h("div", { class: "panel" }, table([
        { label: "Username", render: (u) => h("a", { href: "#", onclick: (e) => { e.preventDefault(); memberDrawer(u.id); } }, u.username) },
        { label: "Email", render: (u) => u.emailAddress },
        { label: "Role", render: (u) => u.role === "librarian" ? "Librarian" : "Member" },
        { label: "Status", render: (u) => h("span", null, h("span", { class: "badge " + (u.isActive ? "green" : "red") }, u.isActive ? "Active" : "Deactivated"), " ", u.isVerified ? null : h("span", { class: "badge amber" }, "Not verified")) },
        { label: "Joined", render: (u) => fmtDate(u.createdAt) },
        { label: "", class: "actions", render: (u) => [
          h("button", { class: "btn secondary sm", type: "button", onclick: () => memberDrawer(u.id) }, "Details"),
          u.isActive
            ? h("button", { class: "btn danger sm", type: "button", onclick: () => toggle(u, false) }, "Deactivate")
            : h("button", { class: "btn sm", type: "button", onclick: () => toggle(u, true) }, "Activate")] },
      ], page.content, empty("No members")), pager(page, (p) => { state.page = p; load(); })));
    } catch (error) { replace(list, empty("Could not load members", error.message)); }
  }

  async function toggle(user, activate) {
    if (!activate && !(await confirmBox("Deactivate " + user.username + "?", "They will not be able to sign in or borrow until you activate them again.", "Deactivate", true))) return;
    try {
      if (activate) await API.activateUser(user.id); else await API.deactivateUser(user.id);
      toast(activate ? "Member activated" : "Member deactivated");
      load();
    } catch (error) { toastError(error); }
  }

  load();
  return h("div", null, form, list);
}

async function memberDrawer(userId) {
  const body = h("div", null, spinner());
  const box = drawer("Member", null, body);
  try {
    const [user, stats, loans, fines] = await Promise.all([
      API.user(userId), API.userStats(userId).catch(() => null),
      API.userLoans(userId, { size: 5 }).catch(() => null), API.userFines(userId).catch(() => []),
    ]);
    const p = user.profile || {};
    box.panel.querySelector("h2").textContent = [p.firstName, p.lastName].filter(Boolean).join(" ") || user.username;
    replace(body, h("div", { class: "stack", style: "gap:22px" },
      h("dl", { class: "kv" },
        h("dt", null, "Username"), h("dd", null, user.username),
        h("dt", null, "Email"), h("dd", null, user.emailAddress),
        h("dt", null, "Role"), h("dd", null, user.role === "librarian" ? "Librarian" : "Member"),
        h("dt", null, "Joined"), h("dd", null, fmtDate(user.createdAt)),
        h("dt", null, "Status"), h("dd", null, user.isActive ? "Active" : "Deactivated", user.isVerified ? "" : ", email not verified")),
      stats ? h("div", { class: "stats" }, stat("Returned", stats.returned), stat("Late", stats.late), stat("On-time rate", stats.reliable ? stats.onTimeRate + "%" : "Too few")) : null,
      h("div", null, h("h3", { style: "margin-bottom:8px" }, "Recent loans"),
        loans && loans.content.length ? table([
          { label: "Book", render: (l) => l.bookTitle }, { label: "Due", render: (l) => fmtDate(l.dueDate) }, { label: "Status", render: (l) => statusBadge(l.status) },
        ], loans.content) : h("p", { class: "muted" }, "No loans yet.")),
      h("div", null, h("h3", { style: "margin-bottom:8px" }, "Fines"),
        fines.length ? table([
          { label: "Reason", render: (f) => f.reason }, { label: "Amount", class: "num", render: (f) => fmtMoney(f.amount) }, { label: "Status", render: (f) => statusBadge(f.status) },
        ], fines) : h("p", { class: "muted" }, "No fines."))));
  } catch (error) { replace(body, empty("Could not load this member", error.message)); }
}

/* ---------- audit log ---------- */

const AUDIT_TYPES = ["USER", "BOOK", "LOAN", "FINE", "REVIEW", "WAITLIST"];
const TYPE_BADGE = { USER: "blue", BOOK: "green", LOAN: "amber", FINE: "red", REVIEW: "", WAITLIST: "blue" };

export async function auditPage(ctx) {
  const state = { type: "", userId: "", sortBy: "timestamp", sortDir: "desc", page: 0, size: 20 };
  const list = h("div");
  const typeBox = select("type", [{ value: "", label: "All records" }, ...AUDIT_TYPES.map((t) => ({ value: t, label: t.charAt(0) + t.slice(1).toLowerCase() }))], "");
  const userBox = input("userId", { type: "number", min: "1", placeholder: "Any user id" });
  const sortBox = select("sortBy", [
    { value: "timestamp", label: "Time" }, { value: "type", label: "Record type" }, { value: "action", label: "Action" }, { value: "userId", label: "User id" }, { value: "id", label: "Entry id" },
  ], "timestamp");
  const dirBox = select("sortDir", [{ value: "desc", label: "Newest first" }, { value: "asc", label: "Oldest first" }], "desc");
  const sizeBox = select("size", [10, 20, 50, 100].map((n) => ({ value: n, label: n + " per page" })), 20);

  const form = h("form", { class: "toolbar", onsubmit: (e) => {
    e.preventDefault();
    state.type = typeBox.value; state.userId = userBox.value.trim(); state.sortBy = sortBox.value; state.sortDir = dirBox.value; state.size = Number(sizeBox.value); state.page = 0;
    load();
  } }, field("Record", typeBox), field("Done by user", userBox), field("Sort by", sortBox), field("Order", dirBox), field("Show", sizeBox),
    h("button", { class: "btn", type: "submit" }, "Apply"),
    h("button", { class: "btn secondary", type: "button", onclick: () => { form.reset(); form.dispatchEvent(new Event("submit", { cancelable: true })); } }, "Reset"));

  async function load() {
    replace(list, spinner());
    try {
      const page = await API.auditLog({ type: state.type, userId: state.userId, page: state.page, size: state.size, sortBy: state.sortBy, sortDir: state.sortDir });
      replace(list, h("div", { class: "panel" }, table([
        { label: "When", render: (a) => fmtDateTime(a.timestamp) },
        { label: "Record", render: (a) => h("span", { class: "badge " + (TYPE_BADGE[a.type] || "") }, a.type) },
        { label: "Action", render: (a) => a.action },
        { label: "Record id", class: "num", render: (a) => a.whatId == null ? "-" : "#" + a.whatId },
        { label: "Done by", render: (a) => a.userId == null ? "System" : h("a", { href: "#", onclick: (e) => { e.preventDefault(); memberDrawer(a.userId); } }, "User " + a.userId) },
        { label: "Entry", class: "num", render: (a) => "#" + a.id },
      ], page.content, empty("No audit entries", "Nothing matches these filters.")), pager(page, (p) => { state.page = p; load(); })));
    } catch (error) { replace(list, empty("Could not load the audit log", error.message)); }
  }

  ctx.onLive(load);
  load();
  return h("div", null,
    h("p", { class: "muted", style: "max-width:70ch;margin-bottom:14px" }, "Every change to users, books, loans, fines, reviews and waitlists is recorded here with who did it and when."),
    form, list);
}
