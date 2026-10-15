# Authentication

The **Supabase** component holds the connection to a project and the signed-in
user. The other components use it through their `Client` property, so their calls
run as that user and row-level security applies.

## Properties

| Property | |
|---|---|
| `ProjectUrl` | `https://<project-ref>.supabase.co` |
| `PublishableKey` | The publishable (`sb_publishable_…`) or legacy `anon` key. Secret and `service_role` keys are refused. |
| `IsSignedIn` | Whether a user is signed in. |
| `CurrentUser` | The signed-in user as a dictionary, or an empty dictionary. |
| `AccessToken` | The user's access token, to call your own servers. Empty when signed out. |

## Signing in

| Block | Result |
|---|---|
| `SignUp(email, password, metadata)` | `SignedUp(user)`. If the project doesn't require email confirmation, the user is also signed in and `SessionChanged` fires. `metadata` is a dictionary saved in the user's `user_metadata`, for example a display name. |
| `SignIn(email, password)` | `SessionChanged(SignedIn, user)` |
| `SignInAnonymously()` | `SessionChanged(SignedIn, user)`. Anonymous sign-ins must be enabled in the project. |
| `SignOut()` | `SessionChanged(SignedOut, …)` |

## Signing in with a code

Users can sign in without a password, with a 6-digit code sent by email or text
message:

| Block | Result |
|---|---|
| `SendEmailCode(email, createUser)` | `MessageSent("SendEmailCode")`: the code is emailed. |
| `SendSmsCode(phone, createUser)` | `MessageSent("SendSmsCode")`: the code is texted. `phone` is in international format, like `+15551234567`. |
| `VerifyEmailCode(email, code, type)` | `SessionChanged(SignedIn, user)`. `type` is a **CodeType** option: `SignIn` for codes from `SendEmailCode`. |
| `VerifySmsCode(phone, code)` | `SessionChanged(SignedIn, user)` |

With `createUser` true, a new email or phone number gets an account; with false,
an unknown one fails with `otp_disabled`. A wrong or expired code fails with
`otp_expired`.

Supabase's emails contain a sign-in link by default, not a code. In the
dashboard, open **Authentication → Emails** and add `{{ .Token }}` to the
**Magic link** template, for example `<p>Your sign-in code: {{ .Token }}</p>`.
Text messages need an SMS provider, set up in **Authentication → Sign In /
Providers → Phone**.

## Resetting a password

1. `ResetPassword(email)` emails a code. `MessageSent("ResetPassword")` fires.
   Add `{{ .Token }}` to the **Reset password** email template as well.
2. `VerifyEmailCode(email, code, PasswordReset)` signs the user in.
3. `UpdateUser("", newPassword, emptyDictionary, "")` sets the new password.

`ResetPassword` succeeds even for an email without an account, so it doesn't
reveal who has one.

Supabase's own email service sends only two emails an hour. Set up your own
SMTP server (Project Settings → Authentication) before your app sends codes to
real users.

## Changing the account

| Block | Result |
|---|---|
| `UpdateUser(email, password, metadata, code)` | `UserUpdated(user)`. Leave `email` or `password` empty, or `metadata` an empty dictionary, to keep them; `metadata` is merged into the saved metadata. |
| `ResendConfirmation(email)` | `MessageSent("ResendConfirmation")`: the sign-up confirmation email is sent again. |
| `Reauthenticate()` | `MessageSent("Reauthenticate")`: the user gets a code by email, or by text message for a phone account. |

A new email is used only after the user confirms it through the link sent to it,
when the project asks for email confirmation.

When **Secure password change** is on in the project's Auth settings, a user who
signed in more than 24 hours ago needs a code to change their password: call
`Reauthenticate`, ask for the code, and pass it as `code`. Otherwise leave `code`
empty.

Anonymous users can keep their data: `UpdateUser` with an email, then with a
password, makes the anonymous account a full one with the same `id`.

## SessionChanged

`SessionChanged(status, user)` is the one place to react to who is signed in.
`status` is one of the **SessionStatus** dropdown's options:

- `SignedIn`: a user is signed in. Also fires when the app starts with a saved
  session, and when the session is refreshed.
- `SignedOut`: nobody is signed in. Also fires when the app starts without a saved
  session.
- `RefreshFailed`: the saved session couldn't be refreshed, usually because the
  device is offline.

Since `SignedIn` can fire more than once, guard one-time setup (like subscribing to
a channel) with a global variable.

## The user dictionary

`user` and `CurrentUser` hold the user as Supabase returns it. Useful keys: `id`,
`email`, `phone`, `user_metadata`, `is_anonymous`, `created_at`,
`last_sign_in_at`. A missing value is an empty text.

## Staying signed in

The session is saved in the app's private storage, per project URL, and restored
when the app starts, in compiled apps and in the AI Companion. It is refreshed
automatically while the app runs.

## Errors

`Error(operation, code, message)`. Common codes: `invalid_credentials`,
`user_already_exists`, `email_not_confirmed`, `weak_password`,
`anonymous_provider_disabled`, `over_request_rate_limit`, `network`. See
[Errors](errors.md).
