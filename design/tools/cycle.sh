#!/bin/sh
set -e
src="${1:-$HOME/Downloads/onme-pleos-integration}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
out="$root/design/cycle"
mkdir -p "$out" "$root/app/src/main/assets/onme"
sed -e 's/#fff4e5/#ffffff/; s/#fff7ec/#ffffff/' \
    -e 's|</head>|<style>header,.state,#hint{display:none!important}canvas,canvas:focus-visible{outline:none!important}canvas{position:fixed!important;inset:0!important;width:100%!important;height:100%!important}</style></head>|' \
    "$src/android/assets/onme/index.html" > "$out/index.html"
cp "$src/THREE-LICENSE" "$out/THREE-LICENSE"
cp "$out/index.html" "$root/app/src/main/assets/onme/index.html"
