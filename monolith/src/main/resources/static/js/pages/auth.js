import { API, session } from "/js/api.js";
import { h, field, input, showFormError, clearFormError, formData, busy, toast } from "/js/ui.js";

const SPINE_COLORS = ["#c4655d", "#d9b25c", "#6fb09d", "#7f9fc3", "#a4879d", "#c98f5f", "#8fb3a4", "#b9695f"];

function spines() {
  const wrap = h("div", { class: "auth-spines", "aria-hidden": "true" });
  for (let i = 0; i < 17; i++) {
    const height = 120 + ((i * 37) % 11) * 12;
    const width = 26 + ((i * 13) % 5) * 7;
    wrap.append(h("i", { style: "height:" + height + "px;width:" + width + "px;background:" + SPINE_COLORS[(i * 3) % SPINE_COLORS.length] + ";animation-delay:" + (i * 45) + "ms" }));
  }
  return wrap;
}

function layout(form) {
  return h("div", { class: "auth" },
    h("section", { class: "auth-art" },
      h("div", { class: "brand" }, h("span", { class: "brand-mark", "aria-hidden": "true" }, h("i"), h("i"), h("i"), h("i")), "BookNest"),
      h("div", null,
        h("h2", null, "Every copy accounted for."),
        h("p", null, "Borrow a book, wait in line for a popular one, and return it for review. Even when everyone wants the same last copy.")),
      spines()),
    h("div", { class: "auth-form-wrap" }, h("div", { class: "auth-form" }, form)));
}

function link(text, href) { return h("a", { href }, text); }

export function loginPage(ctx) {
  const form = h("form", { class: "stack", novalidate: true },
    h("div", null, h("h1", null, "Log in"), h("p", { class: "muted" }, "Use your BookNest account.")),
    field("Username", input("username", { autocomplete: "username", required: true })),
    field("Password", input("password", { type: "password", autocomplete: "current-password", required: true })),
    h("button", { class: "btn block", type: "submit" }, "Log in"),
    h("div", { class: "auth-links" }, link("Create an account", "#/register"), link("Forgot your password?", "#/forgot")));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    const data = formData(form);
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        const result = await API.login({ username: data.username, password: data.password });
        session.set({ token: result.token, user: result.user });
        ctx.navigate("/books");
      } catch (error) {
        if (error.code === "USER_NOT_VERIFIED") {
          toast("Verify your email first", "error", "We will send a new code.");
          ctx.navigate("/verify?u=" + encodeURIComponent(data.username) + "&send=1");
          return;
        }
        showFormError(form, error);
      }
    });
  });
  return layout(form);
}

export function registerPage(ctx) {
  const form = h("form", { class: "stack", novalidate: true },
    h("div", null, h("h1", null, "Create your account"), h("p", { class: "muted" }, "You will confirm your email in the next step.")),
    h("div", { class: "form-grid" },
      field("First name", input("firstName", { autocomplete: "given-name" })),
      field("Last name", input("lastName", { autocomplete: "family-name" })),
      field("Username", input("username", { autocomplete: "username" }), "3 to 15 characters"),
      field("Age", input("age", { type: "number", min: "16", max: "100" }), "You must be 16 or older"),
      h("div", { class: "span2" }, field("Email", input("emailAddress", { type: "email", autocomplete: "email" }))),
      h("div", { class: "span2" }, field("Password", input("password", { type: "password", autocomplete: "new-password" }), "At least 8 characters")),
      h("div", { class: "span2" }, field("Bio", h("textarea", { class: "input", name: "bio", maxlength: "1000" }), "Optional"))),
    h("button", { class: "btn block", type: "submit" }, "Create account"),
    h("div", { class: "auth-links" }, link("I already have an account", "#/login")));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    const data = formData(form);
    data.age = data.age === "" ? null : Number(data.age);
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        await API.register(data);
        toast("Account created", null, "Now confirm your email.");
        ctx.navigate("/verify?u=" + encodeURIComponent(data.username) + "&send=1");
      } catch (error) { showFormError(form, error); }
    });
  });
  return layout(form);
}

export function verifyPage(ctx) {
  const username = ctx.query.u || "";
  const form = h("form", { class: "stack", novalidate: true },
    h("div", null, h("h1", null, "Confirm your email"), h("p", { class: "muted" }, "Enter the code we send to the address on your account.")),
    field("Username", input("username", { value: username, required: true })),
    field("Code", input("code", { inputmode: "numeric", autocomplete: "one-time-code" })),
    h("button", { class: "btn block", type: "submit" }, "Confirm email"),
    h("button", { class: "btn secondary block", type: "button", id: "resend" }, "Send a new code"),
    h("div", { class: "auth-links" }, link("Back to log in", "#/login")));
  async function send() {
    const name = form.querySelector('[name="username"]').value.trim();
    if (!name) { showFormError(form, { message: "Enter your username first." }); return; }
    clearFormError(form);
    try {
      await API.sendVerification(name);
      form.prepend(h("div", { class: "form-ok" }, "Code sent. Check your email."));
    } catch (error) { showFormError(form, error); }
  }
  form.querySelector("#resend").addEventListener("click", (e) => busy(e.currentTarget, send));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    const data = formData(form);
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        const ok = await API.verifyEmail(data.username.trim(), Number(data.code));
        if (ok === false || ok === "false") {
          showFormError(form, { message: "That code is wrong or has expired. Send a new one." });
          return;
        }
        toast("Email confirmed", null, "You can log in now.");
        ctx.navigate("/login");
      } catch (error) { showFormError(form, error); }
    });
  });
  if (ctx.query.send && username) send();
  return layout(form);
}

export function forgotPage() {
  const form = h("form", { class: "stack", novalidate: true },
    h("div", null, h("h1", null, "Reset your password"), h("p", { class: "muted" }, "We will email a reset link to the address on your account.")),
    field("Username", input("username", { autocomplete: "username" })),
    h("button", { class: "btn block", type: "submit" }, "Send reset link"),
    h("div", { class: "auth-links" }, link("Back to log in", "#/login")));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    const data = formData(form);
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        const message = await API.forgotPassword(data.username.trim());
        form.prepend(h("div", { class: "form-ok" }, typeof message === "string" ? message : "If that account exists, a reset email has been sent."));
      } catch (error) { showFormError(form, error); }
    });
  });
  return layout(form);
}

export function resetPage(ctx) {
  const token = ctx.query.token || "";
  const form = h("form", { class: "stack", novalidate: true },
    h("div", null, h("h1", null, "Choose a new password"), h("p", { class: "muted" }, token ? "The link in your email is valid for a short time." : "This page needs the link from your reset email.")),
    field("New password", input("newPassword", { type: "password", autocomplete: "new-password" }), "At least 8 characters"),
    field("Repeat the password", input("again", { type: "password", autocomplete: "new-password" })),
    h("button", { class: "btn block", type: "submit", disabled: token ? undefined : true }, "Save password"),
    h("div", { class: "auth-links" }, link("Back to log in", "#/login")));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormError(form);
    const data = formData(form);
    if (data.newPassword !== data.again) { showFormError(form, { message: "The two passwords are different." }); return; }
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        await API.resetPassword(token, data.newPassword);
        toast("Password changed", null, "Log in with the new password.");
        ctx.navigate("/login");
      } catch (error) { showFormError(form, error); }
    });
  });
  return layout(form);
}
