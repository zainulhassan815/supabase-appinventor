#!/usr/bin/env bash
set -euo pipefail

name=${1:?usage: build-app.sh <name> <form.json> <screen.yail>}
form=$(realpath "${2:?usage: build-app.sh <name> <form.json> <screen.yail>}")
screen=$(realpath "${3:?usage: build-app.sh <name> <form.json> <screen.yail>}")
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
url=${SUPABASE_URL:-http://127.0.0.1:54321}
key=${SUPABASE_KEY:-$(cd "$root" && supabase status -o json 2>/dev/null | python3 -I -c 'import json,sys;print(json.load(sys.stdin)["PUBLISHABLE_KEY"])')}
work=$root/.e2e/$name

(cd "$root" && rush build --no-color >/dev/null)
aix=$(ls "$root"/out/*.aix)
rm -rf "$work" && mkdir -p "$work/project"
python3 -I "$here/app.py" "$work/project" "$aix" "$form" "$screen" "$url" "$key"
"$here/build-apk.sh" "$work"
