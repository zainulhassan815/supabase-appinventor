# Agent instructions

This is an MIT App Inventor extension built with Rush.

Before changing the extension, read `.claude/skills/rush/SKILL.md`.
It explains how blocks, dropdowns, events, dependencies and resources work,
and the limits App Inventor imposes.

## Workflow

1. Run `rush build` after every change and fix every error it reports.
2. Read `out/<package>.md` to check that the blocks match what was asked.
3. Tell the user what to test in App Inventor: `rush build` can't run the
   extension.

## Rules

- Blocks are public methods of the `@Extension` class, annotated with
  `@SimpleFunction`, `@SimpleProperty` or `@SimpleEvent`. Use primitive
  types.
- Dropdown enums go in a directory named `helpers`.
- Fire events on the main thread, never while `Screen.Initialize` runs.
- Declare every library in `rush.yml`.
- Don't edit `.rush/` or `out/`: Rush generates them.

## Project notes

- Calls to Supabase answer through events, never block values: App Inventor
  doesn't wait for a `Continuation` called after the method returns. Data
  blocks take a `tag` that their result and `Error` events carry back.
- Fire startup events from `OnInitializeListener` (or after a post on a
  `ReplForm`), never from a constructor: App Inventor drops events fired before
  `Screen.Initialize`.
- supabase-kt's `-jvm` artifacts only, with kotlin-reflect kept out. Use the
  reified postgrest overloads with `JsonObject` or `List<JsonObject>`.
- `samples/live-notes/schema.sql` is the one schema users run; the local backend
  seeds from it and from `tests/e2e/schema.sql`, which holds tables only the tests
  use.
- Verify changes with the device tests in `docs/testing.md`, not only
  `rush build`.
