#!/usr/bin/env bash
set -euo pipefail

name=${1:?usage: build-app.sh <name> <form.json> <screen.yail>}
form=$(realpath "${2:?usage: build-app.sh <name> <form.json> <screen.yail>}")
screen=$(realpath "${3:?usage: build-app.sh <name> <form.json> <screen.yail>}")
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
rush_cli=${RUSH_CLI:-$root/../rush-cli}
url=${SUPABASE_URL:-http://127.0.0.1:54321}
key=${SUPABASE_KEY:-$(cd "$root" && supabase status -o json 2>/dev/null | python3 -I -c 'import json,sys;print(json.load(sys.stdin)["PUBLISHABLE_KEY"])')}
work=$root/.e2e/$name

(cd "$root" && rush build --no-color >/dev/null)
aix=$(ls "$root"/out/*.aix)
appinventor=$("$rush_cli/tool/e2e/buildserver.sh")
java11=${JAVA11_HOME:-$(ls -d "$HOME"/.sdkman/candidates/java/11* | head -1)}

rm -rf "$work" && mkdir -p "$work/project" "$work/out" "$work/tmp/lib64" "$work/dex"
python3 -I "$here/app.py" "$work/project" "$aix" "$form" "$screen" "$url" "$key"
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
ls "$work"/out/*.apk
