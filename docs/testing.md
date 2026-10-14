# Testing

The tests build real App Inventor apps with App Inventor's build server and run
them on a device or emulator against a local Supabase.

## Setup

- [Supabase CLI](https://supabase.com/docs/guides/cli) and Docker. Run
  `supabase start` in the repository: it seeds the database from
  `samples/live-notes/schema.sql` and serves the functions in `supabase/functions`.
- A checkout of [rush-cli](https://github.com/zainulhassan815/rush-cli) next to this
  repository (or `RUSH_CLI`), whose `tool/e2e/buildserver.sh` builds App Inventor's
  build server once. Its blocks editor must also be built for the sample test:
  `ant -f blocklyeditor/build.xml BlocklyTestbed` in that App Inventor checkout.
- JDK 11, Node.js, and `npm install` in `tests/e2e/blocks`.
- A device or emulator on `adb`. Tests reach the local backend through
  `adb reverse`.

## Scenarios

```
tests/e2e/run.sh tests/e2e/scenarios/database.yail
```

A scenario is a screen's YAIL. It reports lines into a Label, which are matched
against the scenario's `.expected` patterns (`!unordered` on the first line allows
any order). `database`, `storage`, `channel` and `functions` cover each component.

## The sample

```
tests/e2e/live-notes.sh
```

Packs `LiveNotes.aia`, fills in the local backend, compiles its blocks to YAIL with
App Inventor's own generator (`tests/e2e/blocks/compile.js`), builds the APK, and
drives it through every feature, acting as a second user from the computer.

## Block images

```
unzip -p out/dev.dreamerslab.supabase.aix dev.dreamerslab.supabase/components.json > components.json
python3 tests/e2e/blocks/palette.py components.json palette
node tests/e2e/blocks/images.js <appinventor> palette/Screen1.scm palette/<id>.bky components.json docs/images/blocks <id>
python3 docs/blocks.py
```

`palette.py` writes a screen with one of each component and a `.bky` file per block,
named by the block's id (like `SupabaseDatabase1.Query` or `helper.QueryField`), and
prints the ids. `images.js` opens each in App Inventor's blocks editor and saves the
image App Inventor exports, which contains the block. Render each block from its own
file. `docs/blocks.py` then writes [Blocks](blocks.md).
