const STORAGE_KEY = "booknest.session";

export const session = {
  get() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || null; } catch (e) { return null; }
  },
  set(value) {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(value)); } catch (e) { memory = value; }
    memory = value;
  },
  clear() {
    memory = null;
    try { localStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
  },
  current() { return memory || this.get(); },
  token() { const s = this.current(); return s ? s.token : null; },
  user() { const s = this.current(); return s ? s.user : null; },
  isLibrarian() { const u = this.user(); return !!u && String(u.role).toLowerCase() === "librarian"; },
};
let memory = null;

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors) {
    super(message || "Something went wrong");
    this.status = status;
    this.code = code || "";
    this.fieldErrors = fieldErrors || null;
  }
}

function buildUrl(path, query) {
  if (!query) return path;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") params.append(key, value);
  });
  const text = params.toString();
  return text ? path + "?" + text : path;
}

export async function api(method, path, options = {}) {
  const { body, query, form, auth = true } = options;
  const headers = { Accept: "application/json" };
  const token = session.token();
  if (auth && token) headers.Authorization = "Bearer " + token;
  let payload;
  if (form) {
    payload = form;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(buildUrl(path, query), { method, headers, body: payload });
  } catch (e) {
    throw new ApiError(0, "NETWORK", "Cannot reach the server. Check that BookNest is running.");
  }

  const type = response.headers.get("content-type") || "";
  let data = null;
  if (response.status !== 204) {
    if (type.includes("json")) {
      try { data = await response.json(); } catch (e) { data = null; }
    } else {
      const text = await response.text();
      try { data = JSON.parse(text); } catch (e) { data = text || null; }
    }
  }

  if (!response.ok) {
    const info = data && typeof data === "object" ? data : {};
    const error = new ApiError(response.status, info.error, info.message, info.fieldErrors);
    if (response.status === 401 && auth && token) {
      window.dispatchEvent(new CustomEvent("booknest:unauthorized"));
    }
    throw error;
  }
  return data;
}

const get = (path, query) => api("GET", path, { query });
const post = (path, body, query) => api("POST", path, { body, query });
const put = (path, body, query) => api("PUT", path, { body, query });
const del = (path) => api("DELETE", path);

export const API = {
  // authentication
  register: (body) => api("POST", "/auth/users/register", { body, auth: false }),
  login: (body) => api("POST", "/auth/users/login", { body, auth: false }),
  resetPassword: (token, newPassword) => api("PUT", "/auth/users/passwordreset/" + encodeURIComponent(token), { body: { newPassword }, auth: false }),
  sendVerification: (username) => api("PUT", "/auth/email/getverification/" + encodeURIComponent(username), { auth: false }),
  verifyEmail: (username, code) => api("PUT", "/auth/email/verify/" + encodeURIComponent(username), { query: { code }, auth: false }),
  forgotPassword: (username) => api("PUT", "/auth/email/forgotpassword", { body: { username }, auth: false }),

  // catalog
  books: (query) => get("/api/books", query),
  book: (id) => get("/api/books/" + id),
  bookByTitle: (title) => get("/api/books/search/" + encodeURIComponent(title)),
  createBook: (body) => post("/api/books", body),
  updateBook: (id, body) => put("/api/books/" + id, body),
  deleteBook: (id) => del("/api/books/" + id),
  authors: () => get("/api/authors"),
  author: (id) => get("/api/authors/" + id),
  authorByName: (name) => get("/api/authors/search/" + encodeURIComponent(name)),
  createAuthor: (body) => post("/api/authors", body),
  updateAuthor: (id, body) => put("/api/authors/" + id, body),
  deleteAuthor: (id) => del("/api/authors/" + id),
  genres: () => get("/api/genres"),
  genreByName: (name) => get("/api/genres/" + encodeURIComponent(name)),
  createGenre: (body) => post("/api/genres", body),
  updateGenre: (id, body) => put("/api/genres/" + id, body),
  deleteGenre: (id) => del("/api/genres/" + id),

  // reviews
  reviews: (bookId) => get("/api/books/" + bookId + "/reviews"),
  addReview: (bookId, body) => post("/api/books/" + bookId + "/reviews", body),
  updateReview: (id, body) => put("/api/reviews/" + id, body),
  deleteReview: (id) => del("/api/reviews/" + id),

  // loans
  borrow: (bookId, duration) => post("/api/loans", { bookId, duration }),
  myLoans: (query) => get("/api/loans/my-loans", query),
  myStats: () => get("/api/loans/my-stats"),
  returnLoan: (id) => put("/api/loans/" + id + "/return"),
  allLoans: (query) => get("/api/loans", query),
  returnsReview: (query) => get("/api/loans/returns-review", query),
  reviewReturn: (id, body) => put("/api/loans/" + id + "/review-return", body),
  cancelLoan: (id) => put("/api/loans/" + id + "/cancel"),
  userLoans: (userId, query) => get("/api/loans/user/" + userId, query),
  userStats: (userId) => get("/api/loans/user/" + userId + "/stats"),

  // waitlist
  joinWaitlist: (bookId) => post("/api/waitlist", { bookId }),
  myWaitlist: () => get("/api/waitlist/my"),
  waitlistEntry: (id) => get("/api/waitlist/" + id),
  leaveWaitlist: (id) => del("/api/waitlist/" + id),
  claimHold: (id, days) => post("/api/waitlist/" + id + "/claim", undefined, { days }),
  bookQueue: (bookId) => get("/api/waitlist/book/" + bookId),

  // fines
  myFines: () => get("/api/fines/my-fines"),
  allFines: () => get("/api/fines"),
  userFines: (userId) => get("/api/fines/user/" + userId),
  payFine: (id) => put("/api/fines/" + id + "/pay"),

  // profile
  profile: () => get("/api/member/profile"),
  updateProfile: (form) => api("PUT", "/api/member/update/profile", { form }),
  changePassword: (currentPassword, newPassword) => put("/api/member/change-password", { currentPassword, newPassword }),

  // librarian
  users: (query) => get("/api/librarian/users", query),
  user: (id) => get("/api/librarian/users/" + id),
  deactivateUser: (id) => put("/api/librarian/users/deactivate/" + id),
  activateUser: (id) => put("/api/librarian/users/activate/" + id),
  auditLog: (query) => get("/api/audit-log", query),

  // concurrency demo (race-demo profile)
  raceLastCopy: (bookId, threads, safe) => post("/api/demo/race/" + bookId, undefined, { threads, safe }),
  raceLoanLimit: (username, threads, safe) => post("/api/demo/race/loan-limit/" + encodeURIComponent(username), undefined, { threads, safe }),
};
