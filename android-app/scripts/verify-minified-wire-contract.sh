#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Fails the build if R8 renamed any field of com.crumbandember.app.data.model.
#
# Those field names ARE the JSON keys Gson emits, so a rename silently turns
# {"email":..,"password":..} into {"a":..,"b":..} and every auth call comes
# back 400 — but only in a minified build, which no debug/dev test exercises.
# Reading mapping.txt is the cheapest way to assert the keep rules in
# app/proguard-rules.pro are actually doing their job.
#
# Usage: scripts/verify-minified-wire-contract.sh <variantDir>
#        e.g. scripts/verify-minified-wire-contract.sh productionRelease
# ---------------------------------------------------------------------------
set -euo pipefail

VARIANT="${1:-productionRelease}"
MAPPING="app/build/outputs/mapping/${VARIANT}/mapping.txt"
PKG="com.crumbandember.app.data.model"

if [ ! -f "$MAPPING" ]; then
  echo "ERROR: $MAPPING not found — run ./gradlew assemble${VARIANT^} first." >&2
  exit 1
fi

fail=0

# 1) Model classes must map to themselves (not to a.b.c).
while IFS= read -r line; do
  from="${line%% -> *}"; to="${line#* -> }"; to="${to%:}"
  if [ "$from" != "$to" ]; then
    echo "FAIL: class obfuscated: $from -> $to" >&2
    fail=1
  fi
done < <(grep -E "^${PKG//./\\.}\..* -> .*:$" "$MAPPING" || true)

# 2) Every field inside those classes must map to itself.
awk -v pkg="$PKG" '
  /^[^ ].* -> .*:$/ { inpkg = (index($0, pkg ".") == 1); next }
  inpkg && /^ / {
    # "    java.lang.String email -> email"  (methods contain "(")
    if ($0 ~ /\(/) next
    n = split($0, parts, " -> ")
    left = parts[1]; right = parts[2]
    sub(/^[ \t]+/, "", left)
    split(left, lt, " ")
    name = lt[length(lt)]
    gsub(/[ \t]/, "", right)
    if (name != right) {
      printf("FAIL: field obfuscated: %s -> %s\n", name, right) > "/dev/stderr"
      bad = 1
    }
  }
  END { exit bad ? 1 : 0 }
' "$MAPPING" || fail=1

# 3) The models must still be present at all (not shrunk away).
if ! grep -q "${PKG}.RegisterRequest -> " "$MAPPING"; then
  echo "FAIL: ${PKG}.RegisterRequest missing from mapping — model was stripped." >&2
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  echo "" >&2
  echo "Wire contract broken in '${VARIANT}'. The APK would send obfuscated JSON keys" >&2
  echo "and every auth/order call would fail with 400. Check app/proguard-rules.pro." >&2
  exit 1
fi

echo "OK: ${PKG}.* survived minification with field names intact (${VARIANT})."
