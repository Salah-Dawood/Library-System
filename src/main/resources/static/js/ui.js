// small helpers: elements, toasts, dialogs, tables, formatting

export function h(tag, props, ...children) {
  const node = document.createElement(tag);
  Object.entries(props || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === false) return;
    if (key === "class") node.className = value;
    else if (key === "html") node.innerHTML = value;
    else if (key.startsWith("on") && typeof value === "function") node.addEventListener(key.slice(2).toLowerCase(), value);
    else if (key === "value") node.value = value;
    else if (value === true) node.setAttribute(key, "");
    else node.setAttribute(key, value);
  });
  append(node, children);
  return node;
}

function append(node, children) {
  children.flat(Infinity).forEach((child) => {
    if (child === null || child === undefined || child === false) return;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  });
}

export function replace(node, ...children) {
  node.replaceChildren();
  append(node, children);
  return node;
}

export function spinner() { return h("div", { class: "spinner", role: "status", "aria-label": "Loading" }); }

export function empty(title, text, action) {
  return h("div", { class: "empty" }, h("h3", null, title), text ? h("p", null, text) : null, action ? h("div", { style: "margin-top:14px" }, action) : null);
}

// icons
const ICONS = {
  book: "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5zm2 14h13M8 7h7",
  pen: "M4 20l4-1 11-11-3-3L5 16l-1 4zm10-13l3 3",
  tag: "M3 12V4h8l9 9-8 8-9-9zm5-5h.01",
  swap: "M7 7h13l-3-3M17 17H4l3 3",
  clock: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zm0-14v5l3 2",
  coin: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zm0-13v8m-2-6h3a1.5 1.5 0 0 1 0 3h-2a1.5 1.5 0 0 0 0 3h3",
  user: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-8 9a8 8 0 0 1 16 0",
  users: "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zm-7 9a7 7 0 0 1 14 0m1-9a3 3 0 1 0 0-6m3 15a6 6 0 0 0-3-5",
  inbox: "M4 13l2-8h12l2 8v6H4v-6zm0 0h5l1 2h4l1-2h5",
  list: "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01",
  shield: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3zm-3 9l2 2 4-4",
  flask: "M9 3h6M10 3v6L4 19a1.5 1.5 0 0 0 1.3 2h13.4a1.5 1.5 0 0 0 1.3-2L14 9V3M7.5 15h9",
  bell: "M6 16V11a6 6 0 1 1 12 0v5l2 2H4l2-2zm4 4a2 2 0 0 0 4 0",
  menu: "M4 6h16M4 12h16M4 18h16",
  logout: "M10 4H5v16h5M15 8l4 4-4 4M19 12H9",
  doc: "M7 3h7l4 4v14H7V3zm7 0v4h4",
};
export function icon(name) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "none");
  svg.setAttribute("stroke", "currentColor");
  svg.setAttribute("stroke-width", "1.7");
  svg.setAttribute("stroke-linecap", "round");
  svg.setAttribute("stroke-linejoin", "round");
  svg.setAttribute("aria-hidden", "true");
  const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
  path.setAttribute("d", ICONS[name] || ICONS.doc);
  svg.append(path);
  return svg;
}

// toasts
export function toast(message, kind, detail) {
  const box = document.getElementById("toasts");
  const node = h("div", { class: "toast " + (kind || ""), role: kind === "error" ? "alert" : "status" },
    h("div", null, h("b", null, message), detail ? h("small", null, detail) : null),
    h("button", { type: "button", "aria-label": "Dismiss", onclick: () => node.remove() }, "×"));
  box.append(node);
  setTimeout(() => node.remove(), kind === "error" ? 8000 : 5000);
}

const FRIENDLY = {
  NO_COPIES_AVAILABLE: "Someone else just took the last copy. You can join the waitlist.",
  LOAN_LIMIT_REACHED: "You already have 5 books on loan. Return one first.",
  COPIES_AVAILABLE: "Copies are available now, so borrow the book instead.",
  WAITLIST_LIMIT_REACHED: "You are already waiting for 5 books.",
  NO_ACTIVE_HOLD: "There is no held copy to claim for this entry.",
  HOLD_EXPIRED: "Your hold ran out before it was claimed.",
  FINE_ALREADY_PAID: "This fine is already paid.",
  CONCURRENT_UPDATE: "Someone changed the same data at the same moment. Try again in a second.",
  NETWORK: "Cannot reach the server. Check that BookNest is running.",
};
export function errorText(error) {
  if (!error) return "Something went wrong";
  return FRIENDLY[error.code] || error.message || "Something went wrong";
}
export function toastError(error) {
  toast(errorText(error), "error", error && error.code && !FRIENDLY[error.code] ? error.code : null);
}

// form errors
export function showFormError(form, error) {
  form.querySelectorAll(".field-error, .form-error").forEach((n) => n.remove());
  const fields = (error && error.fieldErrors) || {};
  let shown = false;
  Object.entries(fields).forEach(([name, message]) => {
    const input = form.querySelector('[name="' + name + '"]');
    if (input) {
      input.closest("label")?.append(h("span", { class: "field-error" }, message));
      shown = true;
    }
  });
  if (!shown || !Object.keys(fields).length) {
    form.prepend(h("div", { class: "form-error", role: "alert" }, errorText(error)));
  }
}

export function clearFormError(form) {
  form.querySelectorAll(".field-error, .form-error, .form-ok").forEach((n) => n.remove());
}

export function field(label, input, hint) {
  return h("label", { class: "field" }, label, input, hint ? h("span", { class: "hint" }, hint) : null);
}

export function input(name, props) {
  return h("input", { class: "input", name, id: undefined, ...(props || {}) });
}

export function select(name, options, selected, props) {
  const node = h("select", { class: "input", name, ...(props || {}) },
    options.map((o) => {
      const opt = typeof o === "object" ? o : { value: o, label: o };
      return h("option", { value: opt.value, selected: String(opt.value) === String(selected) ? true : undefined }, opt.label);
    }));
  return node;
}

export function formData(form) {
  const data = {};
  new FormData(form).forEach((value, key) => { data[key] = value; });
  return data;
}

// run an async action on a button without double clicks
export async function busy(button, action) {
  if (button.disabled) return;
  const label = button.textContent;
  button.disabled = true;
  try { return await action(); } finally { button.disabled = false; button.textContent = label; }
}

// overlays
function overlay(kind, title, subtitle, body, footer, wide) {
  const previous = document.activeElement;
  const close = () => {
    root.remove();
    document.removeEventListener("keydown", onKey);
    if (previous && previous.focus) previous.focus();
  };
  const onKey = (event) => { if (event.key === "Escape") close(); };
  const head = h("div", { class: "ov-head" },
    h("div", null, h("h2", null, title), subtitle ? h("p", { class: "muted small" }, subtitle) : null),
    h("button", { class: "btn ghost sm", type: "button", "aria-label": "Close", onclick: close }, "Close"));
  const panel = kind === "drawer"
    ? h("div", { class: "drawer", role: "dialog", "aria-modal": "true", "aria-label": title }, head, h("div", { class: "ov-body" }, body), footer ? h("div", { class: "ov-foot" }, footer) : null)
    : h("div", { class: "modal" + (wide ? " wide" : ""), role: "dialog", "aria-modal": "true", "aria-label": title }, head, h("div", { class: "ov-body" }, body), footer ? h("div", { class: "ov-foot" }, footer) : null);
  const root = h("div", { class: "overlay" + (kind === "drawer" ? "" : " center"), onclick: (e) => { if (e.target === root) close(); } }, panel);
  document.body.append(root);
  document.addEventListener("keydown", onKey);
  const first = panel.querySelector("input, select, textarea, button.btn:not(.ghost)");
  if (first) first.focus();
  return { close, panel, root };
}
export const drawer = (title, subtitle, body, footer) => overlay("drawer", title, subtitle, body, footer);
export const modal = (title, subtitle, body, footer, wide) => overlay("modal", title, subtitle, body, footer, wide);

export function confirmBox(title, text, confirmLabel, danger) {
  return new Promise((resolve) => {
    let done = false;
    const finish = (value) => { if (!done) { done = true; box.close(); resolve(value); } };
    const cancel = h("button", { class: "btn secondary", type: "button", onclick: () => finish(false) }, "Cancel");
    const ok = h("button", { class: "btn" + (danger ? " danger" : ""), type: "button", onclick: () => finish(true) }, confirmLabel || "Confirm");
    const box = modal(title, null, h("p", null, text), [cancel, ok]);
    box.root.addEventListener("click", (e) => { if (e.target === box.root) finish(false); });
  });
}

// tables
export function table(columns, rows, emptyNode) {
  if (!rows || !rows.length) return emptyNode || empty("Nothing here yet");
  return h("div", { class: "table-wrap" },
    h("table", { class: "table" },
      h("thead", null, h("tr", null, columns.map((c) => h("th", { scope: "col", class: c.class || "" }, c.label)))),
      h("tbody", null, rows.map((row) => h("tr", null, columns.map((c) => h("td", { class: c.class || "" }, c.render(row))))))));
}

export function pager(page, onChange) {
  if (!page || !page.totalElements) return null;
  const from = page.number * page.size + 1;
  const to = Math.min(page.totalElements, (page.number + 1) * page.size);
  return h("div", { class: "pager" },
    h("span", { class: "num" }, from + " to " + to + " of " + page.totalElements),
    h("div", { class: "row" },
      h("button", { class: "btn secondary sm", type: "button", disabled: page.number <= 0 ? true : undefined, onclick: () => onChange(page.number - 1) }, "Previous"),
      h("span", { class: "num" }, "Page " + (page.number + 1) + " of " + Math.max(1, page.totalPages)),
      h("button", { class: "btn secondary sm", type: "button", disabled: page.number + 1 >= page.totalPages ? true : undefined, onclick: () => onChange(page.number + 1) }, "Next")));
}

// formatting
export function fmtDate(value) {
  if (!value) return "-";
  const d = new Date(String(value).length === 10 ? value + "T00:00:00" : value);
  if (isNaN(d)) return String(value);
  return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
export function fmtDateTime(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d)) return String(value);
  return d.toLocaleString(undefined, { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}
export function fmtMoney(value) {
  if (value === null || value === undefined) return "-";
  return Number(value).toLocaleString(undefined, { style: "currency", currency: "USD" });
}
export function timeLeft(value) {
  if (!value) return "";
  const ms = new Date(value) - new Date();
  if (ms <= 0) return "expired";
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  return hours > 0 ? hours + " h " + minutes + " min left" : minutes + " min left";
}
export function initials(user) {
  const name = (user && user.username) || "?";
  return name.slice(0, 2).toUpperCase();
}
export function stars(rating) {
  const n = Math.max(0, Math.min(5, Math.round(Number(rating) || 0)));
  return h("span", { class: "stars", "aria-label": n + " out of 5" }, "★".repeat(n) + "☆".repeat(5 - n));
}

const STATUS_STYLE = {
  BORROWED: ["green", "On loan"], PENDING_REVIEW: ["amber", "Waiting for review"], RETURNED: ["", "Returned"], CANCELLED: ["", "Cancelled"],
  WAITING: ["blue", "Waiting"], HOLD_READY: ["green", "Copy held for you"], FULFILLED: ["", "Claimed"], EXPIRED: ["red", "Expired"],
  PENDING: ["amber", "Unpaid"], PAID: ["green", "Paid"],
  LATE: ["red", "Late"], DAMAGE: ["red", "Damage"],
  EARLY: ["green", "Early"], ON_TIME: ["green", "On time"],
};
export function statusBadge(status) {
  const [color, label] = STATUS_STYLE[status] || ["", status];
  return h("span", { class: "badge " + color }, label);
}

// book spines get a stable cloth colour from the id
const CLOTH = ["#7a2e34", "#27435f", "#2f5a47", "#a9792a", "#4b4a63", "#6b3f5c", "#3d5c63", "#8a5a33", "#5b3a29", "#33506e"];
export function spineStyle(book) {
  const id = Number(book.id) || 0;
  const seed = (id * 2654435761) >>> 0;
  const width = 46 + (seed % 5) * 6;
  const height = 196 + ((seed >>> 4) % 6) * 12;
  return { color: CLOTH[(id + (seed >>> 8)) % CLOTH.length], width, height };
}

// a modal with a form; onSubmit(data, form) should throw on failure and return normally on success
let formCounter = 0;
export function formModal(title, subtitle, nodes, submitLabel, onSubmit, wide) {
  const id = "form-" + (++formCounter);
  const form = h("form", { class: "stack", id, novalidate: true }, nodes);
  const submit = h("button", { class: "btn", type: "submit", form: id }, submitLabel);
  const cancel = h("button", { class: "btn secondary", type: "button", onclick: () => box.close() }, "Cancel");
  const box = modal(title, subtitle, form, [cancel, submit], wide);
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    await busy(submit, async () => {
      try {
        await onSubmit(formData(form), form, box);
        box.close();
      } catch (error) {
        showFormError(form, error);
      }
    });
  });
  return box;
}
