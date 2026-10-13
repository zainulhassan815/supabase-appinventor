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
