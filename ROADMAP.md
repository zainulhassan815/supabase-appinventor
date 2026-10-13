# Roadmap

## Planned

| Feature | Notes |
|---|---|
| Browser sign-in: Google, GitHub and other OAuth providers, SSO, magic links | Opens the browser and returns to the app through a link; needs an activity in the extension's manifest. |
| One-time codes by email and phone, password reset by code | |
| Updating the user: email, password, metadata | |
| Two-factor authentication (TOTP) | |
| Upload progress and resumable uploads for large files | |
| GraphQL queries | |
| Testing on Kodular and other App Inventor based builders | |

## Not planned

| Feature | Why |
|---|---|
| Supabase's Compose sign-in UI | App Inventor apps can't use Jetpack Compose. |
| Native Google and Apple sign-in | Needs newer AndroidX libraries than App Inventor ships. |
| Admin APIs | They need the secret key, which must never be in an app. Use an Edge Function. |
