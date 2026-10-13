#!/usr/bin/env bash
set -euo pipefail

scenario=$(realpath "${1:?usage: run.sh <scenario.yail> [timeout-seconds]}")
timeout=${2:-60}
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
rush_cli=${RUSH_CLI:-$root/../rush-cli}
url=${SUPABASE_URL:-http://127.0.0.1:54321}
key=${SUPABASE_KEY:-$(cd "$root" && supabase status -o json | python3 -I -c 'import json,sys;print(json.load(sys.stdin)["PUBLISHABLE_KEY"])')}
adb=${ADB:-$HOME/Android/sdk/platform-tools/adb}
package=dev.dreamerslab.supabase.e2e
work=$root/.e2e/$(basename "$scenario" .yail)

(cd "$root" && rush build --no-color >/dev/null)
aix=$(ls "$root"/out/*.aix)
appinventor=$("$rush_cli/tool/e2e/buildserver.sh")
java11=${JAVA11_HOME:-$(ls -d "$HOME"/.sdkman/candidates/java/11* | head -1)}

rm -rf "$work" && mkdir -p "$work/project" "$work/out" "$work/tmp/lib64" "$work/dex"
python3 -I "$here/app.py" "$work/project" "$aix" "$scenario" "$url" "$key"
(cd "$work/project" && zip -q -r -D "$work/project.aia" .)
cp "$appinventor/lib/android/tools/linux/lib64/libc++.so" "$work/tmp/lib64/"
if ! "$java11/bin/java" -Djava.io.tmpdir="$work/tmp" -Dfile.encoding=UTF-8 -Djava.awt.headless=true \
  -cp "$appinventor/buildserver/build/run/lib/*" com.google.appinventor.buildserver.Main \
  --inputZipFile "$work/project.aia" --userName e2e --outputDir "$work/out" --dexCacheDir "$work/dex" \
  > "$work/build.log" 2>&1; then
  grep -E 'ERROR|error:' "$work/build.log" | head -5 >&2
  echo "APK build failed, see $work/build.log" >&2
  exit 1
fi

"$adb" uninstall "$package" >/dev/null 2>&1 || true
"$adb" install "$work"/out/*.apk >/dev/null
"$adb" reverse tcp:54321 tcp:54321 >/dev/null
"$adb" logcat -c
"$adb" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
label() {
  "$adb" shell uiautomator dump /sdcard/e2e.xml >/dev/null
  "$adb" shell cat /sdcard/e2e.xml | python3 -I "$here/label.py"
}

deadline=$((SECONDS + timeout))
until label | python3 -I "$here/check.py" "${scenario%.yail}.expected" >/dev/null 2>&1; do
  if "$adb" logcat -d | grep -A15 "FATAL EXCEPTION" | grep -q "Process: $package"; then
    "$adb" logcat -d | grep -A20 "FATAL EXCEPTION" >&2
    exit 1
  fi
  [ $SECONDS -lt $deadline ] || break
  sleep 2
done
"$adb" exec-out screencap -p > "$work/screen.png"
label | python3 -I "$here/check.py" "${scenario%.yail}.expected"
