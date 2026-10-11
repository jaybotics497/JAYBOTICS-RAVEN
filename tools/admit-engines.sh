#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${1:-$HOME/JAYBOTICS-ENGINES}"
LLAMA_EXPECTED=23b0202a189c44a54625aadcb37a946dd1d6278d
WHISPER_EXPECTED=d1be6fde11ac6e0407606b4e42fe72d34add8037
for e in llama.cpp whisper.cpp; do test -d "$SRC/$e" || { echo "missing $SRC/$e" >&2; exit 2; }; done
if test -d "$SRC/llama.cpp/.git"; then test "$(git -C "$SRC/llama.cpp" rev-parse HEAD)" = "$LLAMA_EXPECTED" || { echo 'llama revision mismatch' >&2; exit 3; }; fi
if test -d "$SRC/whisper.cpp/.git"; then test "$(git -C "$SRC/whisper.cpp" rev-parse HEAD)" = "$WHISPER_EXPECTED" || { echo 'whisper revision mismatch' >&2; exit 4; }; fi
mkdir -p "$ROOT/native"
rm -rf "$ROOT/native/llama.cpp" "$ROOT/native/whisper.cpp"
cp -a "$SRC/llama.cpp" "$ROOT/native/llama.cpp"
cp -a "$SRC/whisper.cpp" "$ROOT/native/whisper.cpp"
rm -rf "$ROOT/native/llama.cpp/.git" "$ROOT/native/whisper.cpp/.git"
printf '%s\n' "$LLAMA_EXPECTED  llama.cpp" "$WHISPER_EXPECTED  whisper.cpp" > "$ROOT/native/ENGINE-REVISIONS.txt"
echo 'ENGINE_ADMISSION=PASS'
