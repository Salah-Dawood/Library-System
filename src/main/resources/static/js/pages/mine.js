import { API, session } from "/js/api.js";
import {
  h, replace, spinner, empty, field, input, select, table, pager, formModal, confirmBox,
  toast, toastError, busy, statusBadge, fmtDate, fmtDateTime, fmtMoney, timeLeft, initials, formData, showFormError, clearFormError,
} from "/js/ui.js";

/* ---------- my loans ---------- */

export async function loansPage(ctx) {
  const state = { status: "", page: 0 };
  const stats = h("div");
  const list = h("div");
  const statusBox = select("status", [
    { value: "", label: "All loans" }, { value: "BORROWED", label: "On loan" },
    { value: "PENDING_REVIEW", label: "Waiting for review" }, { value: "RETURNED", label: "Returned" }, { value: "CANCELLED", label: "Cancelled" },
  ], "");
  statusBox.addEventListener("change", () => { state.status = statusBox.value; state.page = 0; load(); });

  async function loadStats() {
    try {
      const s = await API.myStats();
      replace(stats, h("div", { class: "stats" },
        stat("Returned", s.returned), stat("Early", s.early), stat("On time", s.onTime), stat("Late", s.late),
        stat("On-time rate", s.reliable ? s.onTimeRate + "%" : "Not enough returns yet")));
    } catch (error) { replace(stats); }
  }

  async function load() {
    replace(list, spinner());
    try {
      const page = await API.myLoans({ status: state.status, page: state.page, size: 10 });
      replace(list, h("div", { class: "panel" }, table([
        { label: "Book", render: (l) => h("a", { href: "#/books/" + l.bookId }, l.bookTitle) },
        { label: "Borrowed", render: (l) => fmtDate(l.loanDate) },
        { label: "Due", render: (l) => h("span", null, fmtDate(l.dueDate), l.overdue ? h("span", { class: "badge red", style: "margin-left:8px" }, l.daysLate + (l.daysLate === 1 ? " day late" : " days late")) : null) },
        { label: "Returned", render: (l) => l.returnDate ? fmtDate(l.returnDate) : "-" },
        { label: "Status", render: (l) => h("span", null, statusBadge(l.status), l.returnTiming && l.status !== "BORROWED" ? h("span", { class: "muted small", style: "margin-left:8px" }, l.returnTiming === "LATE" ? l.daysLate + " days late" : l.returnTiming === "EARLY" ? "returned early" : "on time") : null) },
        { label: "Fine", class: "num", render: (l) => l.fineTotal && Number(l.fineTotal) > 0 ? fmtMoney(l.fineTotal) : "-" },
        { label: "", class: "actions", render: (l) => l.status === "BORROWED" ? h("button", { class: "btn secondary sm", type: "button", onclick: (e) => giveBack(e, l) }, "Return book") : (l.reviewNotes ? h("span", { class: "muted small", title: l.reviewNotes }, "Note from librarian") : null) },
      ], page.content, empty("No loans here", "Borrow a book from the catalog and it shows up here.")), pager(page, (p) => { state.page = p; load(); })));
    } catch (error) { replace(list, empty("Could not load your loans", error.message)); }
  }

  async function giveBack(event, loan) {
    await busy(event.currentTarget, async () => {
      try { await API.returnLoan(loan.id); toast("Return recorded", null, "A librarian will check the copy."); load(); loadStats(); } catch (error) { toastError(error); }
    });
  }

  ctx.onLive(() => { load(); loadStats(); });
  loadStats(); load();
  return h("div", null, stats, h("div", { class: "toolbar" }, field("Show", statusBox)), list);
}

function stat(label, value) {
  return h("div", { class: "stat" }, h("b", { class: "num" }, String(value)), h("span", null, label));
}

/* ---------- my waitlist ---------- */

export async function waitlistPage(ctx) {
  const holder = h("div");
  async function load() {
    replace(holder, spinner());
    try {
      const rows = await API.myWaitlist();
      replace(holder, h("div", { class: "panel" }, table([
        { label: "Book", render: (e) => h("a", { href: "#/books/" + e.bookId }, e.bookTitle) },
        { label: "Joined", render: (e) => fmtDateTime(e.joinedAt) },
        { label: "Place in line", class: "num", render: (e) => e.status === "WAITING" ? e.position : "-" },
        { label: "Status", render: (e) => h("span", null, statusBadge(e.status), e.status === "HOLD_READY" ? h("span", { class: "muted small", style: "margin-left:8px" }, timeLeft(e.holdExpiresAt)) : null) },
        { label: "", class: "actions", render: (e) => {
          if (e.status === "HOLD_READY") return [
            h("button", { class: "btn sm", type: "button", onclick: () => claim(e) }, "Claim copy"),
            h("button", { class: "btn secondary sm", type: "button", onclick: (ev) => leave(ev, e) }, "Give up")];
          if (e.status === "WAITING") return h("button", { class: "btn secondary sm", type: "button", onclick: (ev) => leave(ev, e) }, "Leave");
          return null;
        } },
      ], rows, empty("You are not on any waitlist", "When every copy of a book is out, you can join its waitlist from the catalog."))));
    } catch (error) { replace(holder, empty("Could not load your waitlist", error.message)); }
  }

  function claim(entry) {
    formModal("Claim your copy", entry.bookTitle, [field("Loan days", input("days", { type: "number", min: "1", max: "30", value: "14" }), "1 to 30")],
      "Claim copy", async (data) => {
        const loan = await API.claimHold(entry.id, Number(data.days));
        toast("Copy claimed", null, "Due " + fmtDate(loan.dueDate) + ".");
        load();
      });
  }

  async function leave(event, entry) {
    await busy(event.currentTarget, async () => {
      try { await API.leaveWaitlist(entry.id); toast("You left the waitlist"); load(); } catch (error) { toastError(error); }
    });
  }

  ctx.onLive(load);
  load();
  return holder;
}

/* ---------- my fines ---------- */

export async function finesPage(ctx) {
  const holder = h("div");
  async function load() {
    replace(holder, spinner());
    try {
      const rows = await API.myFines();
      const owed = rows.filter((f) => f.status === "PENDING").reduce((sum, f) => sum + Number(f.amount), 0);
      replace(holder,
        h("div", { class: "stats" }, stat("Unpaid", fmtMoney(owed)), stat("Fines in total", rows.length)),
        h("div", { class: "panel" }, table([
          { label: "Reason", render: (f) => f.reason },
          { label: "Type", render: (f) => statusBadge(f.type) },
          { label: "Amount", class: "num", render: (f) => fmtMoney(f.amount) },
          { label: "Status", render: (f) => statusBadge(f.status) },
          { label: "", class: "actions", render: (f) => f.status === "PENDING" ? h("button", { class: "btn sm", type: "button", onclick: (e) => pay(e, f) }, "Pay fine") : null },
        ], rows, empty("You have no fines", "Late returns and damaged copies show up here."))));
    } catch (error) { replace(holder, empty("Could not load your fines", error.message)); }
  }
  async function pay(event, fine) {
    if (!(await confirmBox("Pay this fine?", fmtMoney(fine.amount) + " for " + fine.reason + ".", "Pay " + fmtMoney(fine.amount)))) return;
    try { await API.payFine(fine.id); toast("Fine paid"); load(); } catch (error) { toastError(error); }
  }
  ctx.onLive(load);
  load();
  return holder;
}

/* ---------- profile ---------- */

export async function profilePage(ctx) {
  const holder = h("div", { class: "stack", style: "max-width:760px" }, spinner());

  async function load() {
    let me;
    try { me = await API.profile(); } catch (error) { replace(holder, empty("Could not load your profile", error.message)); return; }
    const p = me.profile || {};
    const name = [p.firstName, p.lastName].filter(Boolean).join(" ") || me.username;

    const fileBox = h("input", { class: "input", type: "file", name: "image", accept: "image/*" });
    const profileForm = h("form", { class: "stack", novalidate: true },
      h("div", { class: "form-grid" },
        field("First name", input("firstName", { value: p.firstName || "" })),
        field("Last name", input("lastName", { value: p.lastName || "" })),
        field("Age", input("age", { type: "number", value: p.age || "" })),
        field("Photo", fileBox)),
      field("Bio", Object.assign(h("textarea", { class: "input", name: "bio" }), { value: p.bio || "" }), "Up to 250 characters"),
      h("div", null, h("button", { class: "btn", type: "submit" }, "Save profile")));
    profileForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      clearFormError(profileForm);
      const fd = new FormData();
      const data = formData(profileForm);
      fd.append("firstName", data.firstName);
      fd.append("lastName", data.lastName);
      fd.append("age", data.age);
      fd.append("bio", data.bio || "");
      if (fileBox.files[0]) fd.append("image", fileBox.files[0]);
      await busy(profileForm.querySelector("button[type=submit]"), async () => {
        try {
          await API.updateProfile(fd);
          toast("Profile saved");
          load();
        } catch (error) { showFormError(profileForm, error); }
      });
    });

    const passwordForm = h("form", { class: "stack", novalidate: true },
      h("div", { class: "form-grid" },
        field("Current password", input("currentPassword", { type: "password", autocomplete: "current-password" })),
        field("New password", input("newPassword", { type: "password", autocomplete: "new-password" }), "At least 8 characters")),
      h("div", null, h("button", { class: "btn", type: "submit" }, "Change password")));
    passwordForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      clearFormError(passwordForm);
      const data = formData(passwordForm);
      await busy(passwordForm.querySelector("button[type=submit]"), async () => {
        try {
          await API.changePassword(data.currentPassword, data.newPassword);
          passwordForm.reset();
          toast("Password changed");
        } catch (error) { showFormError(passwordForm, error); }
      });
    });

    replace(holder,
      h("div", { class: "panel panel-pad row", style: "gap:18px" },
        p.imageUrl ? h("img", { src: p.imageUrl, alt: "", style: "width:72px;height:72px;border-radius:50%;object-fit:cover" })
          : h("span", { class: "avatar", style: "width:72px;height:72px;font-size:24px" }, initials(me)),
        h("div", null, h("h2", null, name),
          h("div", { class: "muted" }, me.username + " - " + me.emailAddress),
          h("div", { style: "margin-top:6px" }, h("span", { class: "badge " + (me.role === "librarian" ? "blue" : "") }, me.role === "librarian" ? "Librarian" : "Member"), " ",
            h("span", { class: "muted small" }, "Joined " + fmtDate(me.createdAt))))),
      h("div", { class: "panel panel-pad stack" }, h("h3", null, "Your details"), profileForm),
      h("div", { class: "panel panel-pad stack" }, h("h3", null, "Password"), passwordForm));
  }
  load();
  return holder;
}
