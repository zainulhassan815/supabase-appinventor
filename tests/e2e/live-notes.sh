#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
adb=${ADB:-$HOME/Android/sdk/platform-tools/adb}
package=appinventor.ai_dreamerslab.LiveNotes
url=${SUPABASE_URL:-http://127.0.0.1:54321}
key=${SUPABASE_KEY:-$(cd "$root" && supabase status -o json 2>/dev/null | python3 -I -c 'import json,sys;print(json.load(sys.stdin)["PUBLISHABLE_KEY"])')}
appinventor=$("${RUSH_CLI:-$root/../rush-cli}/tool/e2e/buildserver.sh")
work=$root/.e2e/live-notes
screen=$work/project/src/appinventor/ai_dreamerslab/LiveNotes

(cd "$root" && rush build --no-color >/dev/null && python3 -I samples/package.py out)
rm -rf "$work" && mkdir -p "$work/project"
unzip -q "$root/out/LiveNotes.aia" -d "$work/project"
python3 -I "$here/connect.py" "$screen/Screen1.scm" "$url" "$key"
node "$here/blocks/compile.js" "$appinventor" "$screen/Screen1.scm" "$screen/Screen1.bky" \
  "$work/project/assets/external_comps/dev.dreamerslab.supabase/components.json" "$package.Screen1" "$screen/Screen1.yail"
apk=$("$here/build-apk.sh" "$work")

"$adb" uninstall "$package" >/dev/null 2>&1 || true
"$adb" install "$apk" >/dev/null
"$adb" reverse tcp:54321 tcp:54321 >/dev/null
"$adb" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
python3 -I "$here/live_notes.py" "$package" "$url" "$key" "$work"
