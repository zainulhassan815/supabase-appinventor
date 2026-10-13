#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
adb=${ADB:-$HOME/Android/sdk/platform-tools/adb}
package=dev.dreamerslab.supabase.e2e
url=${SUPABASE_URL:-http://127.0.0.1:54321}
key=${SUPABASE_KEY:-$(cd "$root" && supabase status -o json 2>/dev/null | python3 -I -c 'import json,sys;print(json.load(sys.stdin)["PUBLISHABLE_KEY"])')}

apk=$("$here/build-app.sh" demo "$here/demo/form.json" "$here/demo/notes.yail")
"$adb" uninstall "$package" >/dev/null 2>&1 || true
"$adb" install "$apk" >/dev/null
"$adb" reverse tcp:54321 tcp:54321 >/dev/null
"$adb" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
python3 -I "$here/demo/drive.py" "$url" "$key" "$root/.e2e/demo"
