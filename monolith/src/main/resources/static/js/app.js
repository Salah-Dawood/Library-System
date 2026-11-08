import { session, API } from "/js/api.js";
import { h, icon, replace, spinner, toast, toastError, initials, fmtDateTime } from "/js/ui.js";
import { startNotifications, stopNotifications } from "/js/sse.js";
import * as auth from "/js/pages/auth.js";
import * as catalog from "/js/pages/catalog.js";
import * as mine from "/js/pages/mine.js";
import * as admin from "/js/pages/admin.js";
import * as race from "/js/pages/race.js";

const PUBLIC = [
  { path: "/login", page: auth.loginPage },
  { path: "/register", page: auth.registerPage },
  { path: "/verify", page: auth.verifyPage },
  { path: "/forgot", page: auth.forgotPage },
  { path: "/reset", page: auth.resetPage },
];

const PRIVATE = [
  { path: "/books", title: "Catalog", group: "Library", icon: "book", page: catalog.booksPage },
  { path: "/authors", title: "Authors", group: "Library", icon: "pen", page: catalog.authorsPage },
  { path: "/genres", title: "Genres", group: "Library", icon: "tag", page: catalog.genresPage },
  { path: "/my/loans", title: "My loans", group: "My account", icon: "swap", page: mine.loansPage },
  { path: "/my/waitlist", title: "My waitlist", group: "My account", icon: "clock", page: mine.waitlistPage },
  { path: "/my/fines", title: "My fines", group: "My account", icon: "coin", page: mine.finesPage },
  { path: "/profile", title: "Profile", group: "My account", icon: "user", page: mine.profilePage },
  { path: "/lib/loans", title: "All loans", group: "Librarian", icon: "list", page: admin.loansPage, librarian: true },
  { path: "/lib/returns", title: "Return reviews", group: "Librarian", icon: "inbox", page: admin.returnsPage, librarian: true },
  { path: "/lib/waitlist", title: "Waitlists", group: "Librarian", icon: "clock", page: admin.waitlistPage, librarian: true },
  { path: "/lib/fines", title: "All fines", group: "Librarian", icon: "coin", page: admin.finesPage, librarian: true },
  { path: "/lib/users", title: "Members", group: "Librarian", icon: "users", page: admin.usersPage, librarian: true },
  { path: "/lib/audit", title: "Audit log", group: "Librarian", icon: "shield", page: admin.auditPage, librarian: true },
  { path: "/lib/race", title: "Race lab", group: "Librarian", icon: "flask", page: race.racePage, librarian: true },
];

const root = document.getElementById("app");
let cleanups = [];
let liveHandlers = [];
let notes = [];
let unread = 0;
let streamOn = false;
let shell = null;
let renderToken = 0;

function parseHash() {
  const raw = location.hash.replace(/^#/, "") || "";
  const [path, queryText] = raw.split("?");
  const query = Object.fromEntries(new URLSearchParams(queryText || ""));
  return { path: path || "", query };
}

export function navigate(path) {
  if (location.hash === "#" + path) route();
  else location.hash = path;
}

async function route() {
  document.querySelectorAll(".overlay").forEach((o) => { const b = o.querySelector("[aria-label=Close]"); if (b) b.click(); else o.remove(); });
  const token = ++renderToken;
  cleanups.forEach((fn) => { try { fn(); } catch (e) { /* ignore */ } });
  cleanups = [];
  liveHandlers = [];
  let { path, query } = parseHash();
  const loggedIn = !!session.token();

  const bookMatch = path.match(/^\/books\/(\d+)$/);
  let params = {};
  if (bookMatch) { path = "/books"; params = { bookId: bookMatch[1] }; }

  const publicRoute = PUBLIC.find((r) => r.path === path);
  if (publicRoute) {
    if (loggedIn && path !== "/reset") { navigate("/books"); return; }
    stopNotifications();
    shell = null;
    replace(root, await publicRoute.page({ query, navigate }));
    return;
  }
  if (!loggedIn) { navigate("/login"); return; }

  const entry = PRIVATE.find((r) => r.path === path);
  if (!entry) { navigate("/books"); return; }
  if (entry.librarian && !session.isLibrarian()) { navigate("/books"); return; }

  if (!shell) buildShell();
  setActiveNav(entry);
  document.title = entry.title + " - BookNest";
  shell.title.textContent = entry.title;
  shell.rail.classList.remove("open");
  replace(shell.content, spinner());
  const ctx = {
    query, params, navigate,
    onLive: (fn) => liveHandlers.push(fn),
    onCleanup: (fn) => cleanups.push(fn),
    setTitle: (text) => { shell.title.textContent = text; },
  };
  try {
    const node = await entry.page(ctx);
    if (token === renderToken) replace(shell.content, node);
  } catch (error) {
    if (token === renderToken) {
      replace(shell.content, h("div", { class: "panel panel-pad" }, h("h3", null, "This page could not load"), h("p", { class: "muted" }, error.message)));
    }
  }
}

function buildShell() {
  const user = session.user();
  const nav = h("nav", { class: "nav", "aria-label": "Main" });
  const groups = {};
  PRIVATE.forEach((r) => {
    if (r.librarian && !session.isLibrarian()) return;
    if (!groups[r.group]) {
      groups[r.group] = h("div", { class: "nav-group" }, h("div", { class: "nav-title" }, r.group));
      nav.append(groups[r.group]);
    }
    groups[r.group].append(h("a", { href: "#" + r.path, "data-path": r.path }, icon(r.icon), r.title));
  });

  const rail = h("aside", { class: "rail" },
    h("a", { class: "brand", href: "#/books" }, h("span", { class: "brand-mark", "aria-hidden": "true" }, h("i"), h("i"), h("i"), h("i")), "BookNest"),
    nav);

  const title = h("h1", null, "");
  const bellCount = h("span", { class: "dot-count", hidden: true }, "0");
  const popList = h("div", { class: "pop-list" });
  const pop = h("div", { class: "pop", hidden: true, role: "region", "aria-label": "Notifications" },
    h("div", { class: "pop-head" }, h("b", null, "Notifications"), h("span", { class: "small muted", id: "stream-state" }, "")),
    popList);
  const bell = h("button", { class: "icon-btn", type: "button", "aria-label": "Notifications", onclick: () => {
    pop.hidden = !pop.hidden;
    if (!pop.hidden) { unread = 0; paintBell(); }
  } }, icon("bell"), bellCount);
  document.addEventListener("click", (e) => { if (!bell.parentElement.contains(e.target)) pop.hidden = true; });

  const chip = h("button", { class: "user-chip", type: "button", onclick: () => navigate("/profile"), "aria-label": "Open my profile" },
    h("span", { class: "avatar" }, initials(user)),
    h("div", null, h("b", null, user.username), h("span", null, session.isLibrarian() ? "Librarian" : "Member")));
  const logout = h("button", { class: "icon-btn", type: "button", "aria-label": "Log out", title: "Log out", onclick: logOut }, icon("logout"));

  const topbar = h("header", { class: "topbar" },
    h("button", { class: "icon-btn menu-btn", type: "button", "aria-label": "Open menu", onclick: () => rail.classList.toggle("open") }, icon("menu")),
    title, h("div", { class: "grow" }),
    h("div", { class: "rel" }, bell, pop), chip, logout);
  const content = h("main", { class: "content", id: "content" });
  replace(root, h("div", { class: "app" }, rail, h("div", { class: "main" }, topbar, content)));
  shell = { rail, title, content, bellCount, popList, pop };
  paintBell();
  paintList();

  // the avatar uses the profile picture when there is one
  API.profile().then((p) => {
    if (p && p.imageUrl) chip.querySelector(".avatar").replaceChildren(h("img", { src: p.imageUrl, alt: "" }));
  }).catch(() => {});

  startNotifications(onLive, (on) => {
    streamOn = on;
    const state = document.getElementById("stream-state");
    if (state) state.textContent = on ? "Live" : "Reconnecting";
  });
}

function setActiveNav(entry) {
  shell.rail.querySelectorAll("a[data-path]").forEach((a) => {
    if (a.dataset.path === entry.path) a.setAttribute("aria-current", "page");
    else a.removeAttribute("aria-current");
  });
}

const EVENT_TITLES = {
  HOLD_READY: "A copy is held for you",
  HOLD_EXPIRED: "Your hold expired",
  LOAN_RETURNED: "Return received",
  RETURN_REVIEWED: "Your return was reviewed",
  LOAN_CANCELLED: "A loan was cancelled",
};

function onLive(event) {
  if (!event || !event.type || ["CONNECTED", "heartbeat", "HEARTBEAT"].includes(event.type)) return;
  const title = EVENT_TITLES[event.type] || String(event.type || "Update").replaceAll("_", " ").toLowerCase().replace(/^./, (c) => c.toUpperCase());
  notes.unshift({ title, message: event.message, time: event.timestamp || new Date().toISOString() });
  notes = notes.slice(0, 30);
  unread += 1;
  paintBell();
  paintList();
  toast(title, "live", event.message);
  liveHandlers.forEach((fn) => { try { fn(event); } catch (e) { /* ignore */ } });
}

function paintBell() {
  if (!shell) return;
  shell.bellCount.hidden = unread === 0;
  shell.bellCount.textContent = String(unread);
}
function paintList() {
  if (!shell) return;
  replace(shell.popList, notes.length
    ? notes.map((n) => h("div", { class: "pop-item" }, h("b", null, n.title), n.message ? h("div", null, n.message) : null, h("small", null, fmtDateTime(n.time))))
    : h("div", { class: "pop-item muted" }, "Nothing yet. Live updates appear here."));
}

function logOut() {
  stopNotifications();
  session.clear();
  shell = null;
  notes = [];
  unread = 0;
  navigate("/login");
}

window.addEventListener("booknest:unauthorized", () => {
  if (!session.token()) return;
  toast("Your session ended. Log in again.", "error");
  logOut();
});
window.addEventListener("hashchange", route);
route();
export { toastError };
