import { API, session } from "/js/api.js";
import {
  h, replace, spinner, empty, field, input, select, table, pager, drawer, modal, formModal, confirmBox,
  toast, toastError, busy, stars, statusBadge, fmtDate, fmtDateTime, timeLeft, spineStyle, formData, showFormError, clearFormError,
} from "/js/ui.js";

const SORTS = [
  { value: "id", label: "Newest added" }, { value: "title", label: "Title" }, { value: "publishedYear", label: "Year published" },
];

/* ---------- books ---------- */

export async function booksPage(ctx) {
  const librarian = session.isLibrarian();
  const state = { title: "", genre: "", sortBy: "title", sortDir: "asc", page: 0, view: "shelf" };
  const genres = await API.genres().catch(() => []);
  const results = h("div");

  const titleBox = input("title", { type: "search", placeholder: "Search by title" });
  const genreBox = select("genre", [{ value: "", label: "All genres" }, ...genres.map((g) => ({ value: g.name, label: g.name }))], "");
  const sortBox = select("sortBy", SORTS, state.sortBy);
  const dirBox = select("sortDir", [{ value: "asc", label: "A to Z" }, { value: "desc", label: "Z to A" }], "asc");

  const shelfBtn = h("button", { type: "button", "aria-pressed": "true", onclick: () => setView("shelf") }, "Shelf");
  const tableBtn = h("button", { type: "button", "aria-pressed": "false", onclick: () => setView("table") }, "List");
  function setView(view) {
    state.view = view;
    state.page = 0;
    shelfBtn.setAttribute("aria-pressed", String(view === "shelf"));
    tableBtn.setAttribute("aria-pressed", String(view === "table"));
    load();
  }

  const exactBtn = h("button", { class: "btn secondary", type: "button", onclick: async (e) => {
    const title = titleBox.value.trim();
    if (!title) { toast("Type the full title first", "error"); return; }
    await busy(e.currentTarget, async () => {
      try { const book = await API.bookByTitle(title); openBook(book.id, load, ctx); } catch (error) { toastError(error); }
    });
  } }, "Open exact title");

  const toolbar = h("form", { class: "toolbar", onsubmit: (e) => { e.preventDefault(); apply(); } },
    h("div", { class: "grow", style: "min-width:220px" }, field("Title", titleBox)),
    field("Genre", genreBox), field("Sort by", sortBox), field("Order", dirBox),
    h("button", { class: "btn", type: "submit" }, "Search"), exactBtn,
    h("div", { class: "seg", role: "group", "aria-label": "View" }, shelfBtn, tableBtn),
    librarian ? h("button", { class: "btn secondary", type: "button", onclick: () => bookForm(null, load) }, "Add book") : null);

  function apply() {
    state.title = titleBox.value.trim();
    state.genre = genreBox.value;
    state.sortBy = sortBox.value;
    state.sortDir = dirBox.value;
    state.page = 0;
    load();
  }

  async function load() {
    replace(results, spinner());
    try {
      const page = await API.books({
        title: state.title, genre: state.genre, sortBy: state.sortBy, sortDir: state.sortDir,
        page: state.page, size: state.view === "shelf" ? 24 : 10,
      });
      if (!page.content.length) { replace(results, h("div", { class: "panel" }, empty("No books found", "Try a shorter title or a different genre."))); return; }
      replace(results, state.view === "shelf" ? shelf(page) : list(page));
    } catch (error) { replace(results, h("div", { class: "panel" }, empty("Could not load books", error.message))); }
  }

  function shelf(page) {
    return h("div", null,
      h("div", { class: "shelf-legend" },
        h("span", null, h("i"), "Copy on the shelf"), h("span", null, h("i", { class: "out" }), "Copy out on loan"),
        h("span", null, "Select a spine to open the book")),
      h("div", { class: "shelf", role: "list" }, page.content.map(spine)),
      h("div", { class: "shelf-board" }),
      pager(page, (p) => { state.page = p; load(); }));
  }

  function spine(book) {
    const s = spineStyle(book);
    const dots = [];
    const shown = Math.min(book.totalCopies, 6);
    for (let i = 0; i < shown; i++) dots.push(h("i", { class: i < book.availableCopies ? "" : "out" }));
    const sold = book.availableCopies < 1;
    return h("button", {
      class: "spine" + (sold ? " sold" : ""), type: "button", role: "listitem",
      style: "background:" + s.color + ";width:" + s.width + "px;height:" + s.height + "px",
      title: book.title + " by " + book.authorName,
      "aria-label": book.title + " by " + book.authorName + ", " + book.availableCopies + " of " + book.totalCopies + " available",
      onclick: () => openBook(book.id, load, ctx),
    }, h("span", { class: "sd" }, dots), h("span", { class: "st" }, book.title), h("span", { class: "sa" }, book.authorName));
  }

  function list(page) {
    const columns = [
      { label: "Title", render: (b) => h("a", { href: "#/books/" + b.id }, b.title) },
      { label: "Author", render: (b) => b.authorName },
      { label: "Genres", render: (b) => (b.genreNames || []).join(", ") },
      { label: "Year", class: "num", render: (b) => b.publishedYear },
      { label: "Rating", render: (b) => b.reviewCount ? h("span", null, stars(b.averageRating), " ", h("span", { class: "muted small" }, "(" + b.reviewCount + ")")) : h("span", { class: "muted" }, "No reviews") },
      { label: "Copies", render: (b) => h("span", { class: "badge " + (b.availableCopies ? "green" : "red") }, b.availableCopies + " of " + b.totalCopies + " free") },
    ];
    return h("div", { class: "panel" }, table(columns, page.content), pager(page, (p) => { state.page = p; load(); }));
  }

  ctx.onLive(() => load());
  load();
  if (ctx.params.bookId) openBook(Number(ctx.params.bookId), load, ctx);
  return h("div", null, toolbar, results);
}

/* ---------- one book ---------- */

async function openBook(bookId, onChanged, ctx) {
  const librarian = session.isLibrarian();
  const body = h("div", null, spinner());
  const box = drawer("Book", null, body);
  box.root.addEventListener("click", () => {});
  async function draw() {
    let book, mine, summary;
    try {
      [book, mine, summary] = await Promise.all([API.book(bookId), API.myWaitlist().catch(() => []), API.reviews(bookId).catch(() => null)]);
    } catch (error) { replace(body, empty("This book could not be loaded", error.message)); return; }
    box.panel.querySelector("h2").textContent = book.title;
    box.panel.querySelector(".ov-head p")?.remove();
    box.panel.querySelector(".ov-head > div").append(h("p", { class: "muted small" }, "by " + book.authorName + ", " + book.publishedYear));
    const entry = mine.find((e) => e.bookId === book.id && (e.status === "WAITING" || e.status === "HOLD_READY"));
    const free = book.availableCopies;
    const sections = [];

    sections.push(h("div", null,
      h("div", { class: "row between", style: "margin-bottom:6px" },
        h("b", null, free + " of " + book.totalCopies + " copies on the shelf"),
        book.reviewCount ? h("span", null, stars(book.averageRating), " ", h("span", { class: "muted small" }, book.averageRating?.toFixed(1))) : null),
      h("div", { class: "stockbar", role: "img", "aria-label": free + " of " + book.totalCopies + " available" },
        h("i", { style: "width:" + (book.totalCopies ? (free / book.totalCopies) * 100 : 0) + "%;background:var(--accent)" })),
      h("dl", { class: "kv", style: "margin-top:16px" },
        h("dt", null, "ISBN"), h("dd", { class: "num" }, book.isbn),
        h("dt", null, "Genres"), h("dd", null, (book.genreNames || []).join(", ") || "-"))));

    sections.push(actionBlock(book, entry));
    if (librarian) sections.push(librarianBlock(book));
    sections.push(reviewBlock(book, summary));
    replace(body, h("div", { class: "stack", style: "gap:26px" }, sections));
  }

  function actionBlock(book, entry) {
    if (entry && entry.status === "HOLD_READY") {
      const days = input("days", { type: "number", min: "1", max: "30", value: "14", style: "width:90px" });
      return h("div", { class: "panel panel-pad stack", style: "background:var(--accent-soft);border-color:#b9d6cc" },
        h("h3", null, "A copy is held for you"),
        h("p", null, "Claim it before the hold ends: " + timeLeft(entry.holdExpiresAt) + " (" + fmtDateTime(entry.holdExpiresAt) + ")."),
        h("div", { class: "row wrap" }, field("Loan days", days),
          h("button", { class: "btn", type: "button", onclick: (e) => busy(e.currentTarget, async () => {
            try { await API.claimHold(entry.id, Number(days.value)); toast("Copy claimed", null, "It is now on your loans."); onChanged(); draw(); } catch (error) { toastError(error); draw(); }
          }) }, "Claim the copy"),
          h("button", { class: "btn secondary", type: "button", onclick: (e) => leave(e, entry) }, "Give it up")));
    }
    if (entry) {
      return h("div", { class: "panel panel-pad stack" },
        h("h3", null, "You are waiting for this book"),
        h("p", null, entry.position > 0 ? "You are number " + entry.position + " in line." : "Your place in line is being updated."),
        h("div", null, h("button", { class: "btn secondary", type: "button", onclick: (e) => leave(e, entry) }, "Leave the waitlist")));
    }
    if (book.availableCopies > 0) {
      const days = input("duration", { type: "number", min: "1", max: "30", value: "14", style: "width:90px" });
      return h("div", { class: "panel panel-pad stack" },
        h("h3", null, "Borrow this book"),
        h("div", { class: "row wrap", style: "align-items:flex-end" }, field("Loan days", days, "1 to 30"),
          h("button", { class: "btn", type: "button", onclick: (e) => busy(e.currentTarget, async () => {
            try {
              const loan = await API.borrow(book.id, Number(days.value));
              toast("Borrowed", null, "Due " + fmtDate(loan.dueDate) + ".");
              onChanged(); draw();
            } catch (error) { toastError(error); draw(); onChanged(); }
          }) }, "Borrow")));
    }
    return h("div", { class: "panel panel-pad stack" },
      h("h3", null, "All copies are out"),
      h("p", { class: "muted" }, "Join the waitlist. When a copy comes back it is held for the first person in line."),
      h("div", null, h("button", { class: "btn", type: "button", onclick: (e) => busy(e.currentTarget, async () => {
        try { await API.joinWaitlist(book.id); toast("You joined the waitlist"); draw(); } catch (error) { toastError(error); draw(); }
      }) }, "Join the waitlist")));
  }

  async function leave(event, entry) {
    await busy(event.currentTarget, async () => {
      try { await API.leaveWaitlist(entry.id); toast("You left the waitlist"); onChanged(); draw(); } catch (error) { toastError(error); }
    });
  }

  function librarianBlock(book) {
    const queue = h("div");
    return h("div", { class: "stack" },
      h("h3", null, "Librarian tools"),
      h("div", { class: "row wrap" },
        h("button", { class: "btn secondary", type: "button", onclick: () => bookForm(book, () => { onChanged(); draw(); }) }, "Edit book"),
        h("button", { class: "btn secondary", type: "button", onclick: async (e) => {
          await busy(e.currentTarget, async () => {
            try {
              const rows = await API.bookQueue(book.id);
              replace(queue, rows.length ? table([
                { label: "Place", class: "num", render: (r) => r.status === "HOLD_READY" ? "Holding" : r.position },
                { label: "Member", render: (r) => r.username },
                { label: "Status", render: (r) => statusBadge(r.status) },
                { label: "Joined", render: (r) => fmtDateTime(r.joinedAt) },
              ], rows) : h("p", { class: "muted" }, "Nobody is waiting for this book."));
            } catch (error) { toastError(error); }
          });
        } }, "Show waitlist"),
        h("button", { class: "btn danger", type: "button", onclick: async () => {
          if (!(await confirmBox("Delete this book?", "\"" + book.title + "\" will be removed from the catalog.", "Delete book", true))) return;
          try { await API.deleteBook(book.id); toast("Book deleted"); box.close(); onChanged(); } catch (error) { toastError(error); }
        } }, "Delete book")),
      queue);
  }

  function reviewBlock(book, summary) {
    if (!summary) return h("div");
    const mineReview = summary.reviews.find((r) => r.mine);
    const form = reviewForm(book, mineReview, summary.canReview);
    return h("div", { class: "stack" },
      h("div", { class: "row between" }, h("h3", null, "Reviews"), summary.reviewCount ? h("span", { class: "muted small" }, summary.reviewCount + (summary.reviewCount === 1 ? " review" : " reviews")) : null),
      summary.reviews.length ? h("div", null, summary.reviews.map((r) => h("div", { class: "review" },
        h("div", { class: "row between" }, h("span", null, h("b", null, r.username), " ", stars(r.rating)), h("span", { class: "muted small" }, fmtDate(r.createdAt))),
        r.comment ? h("p", { style: "margin-top:4px" }, r.comment) : null,
        r.mine || librarian ? h("div", { class: "row", style: "margin-top:6px" },
          r.mine ? null : null,
          h("button", { class: "btn ghost sm", type: "button", onclick: async () => {
            if (!(await confirmBox("Delete this review?", "This cannot be undone.", "Delete review", true))) return;
            try { await API.deleteReview(r.id); toast("Review deleted"); onChanged(); draw(); } catch (error) { toastError(error); }
          } }, "Delete")) : null)))
        : h("p", { class: "muted" }, "No reviews yet."),
      form);
  }

  function reviewForm(book, existing, canReview) {
    if (!existing && !canReview) return h("p", { class: "muted small" }, "You can review a book after you have returned it.");
    const rating = select("rating", [1, 2, 3, 4, 5].map((n) => ({ value: n, label: n + " out of 5" })), existing ? existing.rating : 5);
    const comment = h("textarea", { class: "input", name: "comment" });
    comment.value = existing && existing.comment ? existing.comment : "";
    const form = h("form", { class: "stack", novalidate: true },
      h("b", null, existing ? "Edit your review" : "Write a review"),
      field("Rating", rating), field("Comment", comment),
      h("div", null, h("button", { class: "btn", type: "submit" }, existing ? "Save review" : "Post review")));
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      clearFormError(form);
      const data = formData(form);
      await busy(form.querySelector("button[type=submit]"), async () => {
        try {
          const payload = { rating: Number(data.rating), comment: data.comment };
          if (existing) await API.updateReview(existing.id, payload); else await API.addReview(book.id, payload);
          toast(existing ? "Review saved" : "Review posted");
          onChanged(); draw();
        } catch (error) { showFormError(form, error); }
      });
    });
    return form;
  }

  draw();
}

/* ---------- book form (librarian) ---------- */

async function bookForm(book, onDone) {
  let authors = [], genres = [];
  try { [authors, genres] = await Promise.all([API.authors(), API.genres()]); } catch (error) { toastError(error); return; }
  if (!authors.length || !genres.length) { toast("Add an author and a genre first", "error"); return; }
  const chosen = new Set(book ? book.genreIds || [] : []);
  const title = input("title", { value: book ? book.title : "" });
  const isbn = input("isbn", { value: book ? book.isbn : "" });
  const year = input("publishedYear", { type: "number", value: book ? book.publishedYear : "" });
  const copies = input("totalCopies", { type: "number", min: "0", value: book ? book.totalCopies : 1 });
  const author = select("authorId", authors.filter((a) => a.active !== false).map((a) => ({ value: a.id, label: a.name })), book ? book.authorId : "");
  const genreBoxes = genres.map((g) => h("label", { class: "check" },
    h("input", { type: "checkbox", value: g.id, checked: chosen.has(g.id) ? true : undefined }), g.name));
  formModal(book ? "Edit book" : "Add a book", null, [
    h("div", { class: "form-grid" },
      h("div", { class: "span2" }, field("Title", title)),
      field("ISBN", isbn), field("Year published", year), field("Author", author), field("Total copies", copies, book ? "Changing this adds or removes copies" : null),
      h("div", { class: "span2" }, h("fieldset", { style: "border:0;padding:0;margin:0" }, h("legend", { style: "font-weight:600;font-size:14px;margin-bottom:6px" }, "Genres"),
        h("div", { class: "form-grid" }, genreBoxes))))
  ], book ? "Save changes" : "Add book", async (data, form) => {
    const ids = [...form.querySelectorAll('input[type=checkbox]:checked')].map((c) => Number(c.value));
    const payload = {
      title: data.title, isbn: data.isbn, publishedYear: data.publishedYear === "" ? null : Number(data.publishedYear),
      authorId: Number(data.authorId), totalCopies: data.totalCopies === "" ? null : Number(data.totalCopies), genreIds: ids,
    };
    if (book) await API.updateBook(book.id, payload); else await API.createBook(payload);
    toast(book ? "Book saved" : "Book added");
    onDone();
  }, true);
}

/* ---------- authors ---------- */

export async function authorsPage(ctx) {
  const librarian = session.isLibrarian();
  const holder = h("div");
  const search = input("name", { type: "search", placeholder: "Exact author name" });

  async function load(rows) {
    replace(holder, spinner());
    try {
      const authors = rows || await API.authors();
      replace(holder, h("div", { class: "panel" }, table([
        { label: "Name", render: (a) => a.name },
        { label: "Born", class: "num", render: (a) => a.birthYear },
        { label: "Nationality", render: (a) => a.nationality },
        { label: "Status", render: (a) => h("span", { class: "badge " + (a.active === false ? "" : "green") }, a.active === false ? "Retired" : "Active") },
        librarian ? { label: "", class: "actions", render: (a) => [
          h("button", { class: "btn secondary sm", type: "button", onclick: () => authorForm(a, () => load()) }, "Edit"),
          h("button", { class: "btn danger sm", type: "button", onclick: async () => {
            if (!(await confirmBox("Delete " + a.name + "?", "Authors with books may not be deletable.", "Delete author", true))) return;
            try { await API.deleteAuthor(a.id); toast("Author deleted"); load(); } catch (error) { toastError(error); }
          } }, "Delete")] } : null,
      ].filter(Boolean), authors, empty("No authors yet", librarian ? "Add the first author." : null))));
    } catch (error) { replace(holder, empty("Could not load authors", error.message)); }
  }

  const bar = h("form", { class: "toolbar", onsubmit: async (e) => {
    e.preventDefault();
    if (!search.value.trim()) { load(); return; }
    try { load([await API.authorByName(search.value.trim())]); } catch (error) { toastError(error); }
  } }, h("div", { class: "grow", style: "min-width:220px" }, field("Find an author", search)),
    h("button", { class: "btn", type: "submit" }, "Search"),
    h("button", { class: "btn secondary", type: "button", onclick: () => { search.value = ""; load(); } }, "Show all"),
    librarian ? h("button", { class: "btn secondary", type: "button", onclick: () => authorForm(null, () => load()) }, "Add author") : null);
  load();
  return h("div", null, bar, holder);
}

function authorForm(author, onDone) {
  formModal(author ? "Edit author" : "Add an author", null, [
    field("Name", input("name", { value: author ? author.name : "" })),
    h("div", { class: "form-grid" },
      field("Birth year", input("birthYear", { type: "number", value: author ? author.birthYear : "" })),
      field("Nationality", input("nationality", { value: author ? author.nationality : "" })))],
  author ? "Save changes" : "Add author", async (data) => {
    const payload = { name: data.name, birthYear: data.birthYear === "" ? null : Number(data.birthYear), nationality: data.nationality };
    if (author) await API.updateAuthor(author.id, payload); else await API.createAuthor(payload);
    toast(author ? "Author saved" : "Author added");
    onDone();
  });
}

/* ---------- genres ---------- */

export async function genresPage(ctx) {
  const librarian = session.isLibrarian();
  const holder = h("div");
  const search = input("name", { type: "search", placeholder: "Exact genre name" });

  async function load(rows) {
    replace(holder, spinner());
    try {
      const genres = rows || await API.genres();
      replace(holder, h("div", { class: "panel" }, table([
        { label: "Name", render: (g) => g.name },
        { label: "Description", render: (g) => g.description },
        librarian ? { label: "", class: "actions", render: (g) => [
          h("button", { class: "btn secondary sm", type: "button", onclick: () => genreForm(g, () => load()) }, "Edit"),
          h("button", { class: "btn danger sm", type: "button", onclick: async () => {
            if (!(await confirmBox("Delete " + g.name + "?", "The genre is removed from the catalog.", "Delete genre", true))) return;
            try { await API.deleteGenre(g.id); toast("Genre deleted"); load(); } catch (error) { toastError(error); }
          } }, "Delete")] } : null,
      ].filter(Boolean), genres, empty("No genres yet", librarian ? "Add the first genre." : null))));
    } catch (error) { replace(holder, empty("Could not load genres", error.message)); }
  }

  const bar = h("form", { class: "toolbar", onsubmit: async (e) => {
    e.preventDefault();
    if (!search.value.trim()) { load(); return; }
    try { load([await API.genreByName(search.value.trim())]); } catch (error) { toastError(error); }
  } }, h("div", { class: "grow", style: "min-width:220px" }, field("Find a genre", search)),
    h("button", { class: "btn", type: "submit" }, "Search"),
    h("button", { class: "btn secondary", type: "button", onclick: () => { search.value = ""; load(); } }, "Show all"),
    librarian ? h("button", { class: "btn secondary", type: "button", onclick: () => genreForm(null, () => load()) }, "Add genre") : null);
  load();
  return h("div", null, bar, holder);
}

function genreForm(genre, onDone) {
  const description = h("textarea", { class: "input", name: "description" });
  description.value = genre ? genre.description || "" : "";
  formModal(genre ? "Edit genre" : "Add a genre", null, [
    field("Name", input("name", { value: genre ? genre.name : "" })),
    field("Description", description)],
  genre ? "Save changes" : "Add genre", async (data) => {
    if (genre) await API.updateGenre(genre.id, data); else await API.createGenre(data);
    toast(genre ? "Genre saved" : "Genre added");
    onDone();
  });
}
