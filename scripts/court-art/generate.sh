#!/bin/zsh

# Regenerates the court card drawables from Dmitry Fomin's English pattern playing cards (CC0, Wikimedia Commons).
# Needs node and a network connection; npm keeps the converter's two libraries in this folder.

set -e

cd "$(dirname "$0")"

SVGS=$(mktemp -d)
trap 'rm -rf "$SVGS"' EXIT

# Wikimedia turns away requests that don't say who is asking
for rank in jack queen king; do
    for suit in spades hearts diamonds clubs; do
        curl -fsSL -A "sp21-ace court-art (https://github.com/aQuigs/sp21-ace)" -o "$SVGS/${rank}_$suit.svg" \
            "https://commons.wikimedia.org/wiki/Special:FilePath/English_pattern_${rank}_of_$suit.svg"
    done
done

npm install --silent --no-package-lock
node convert.mjs "$SVGS" ../../app/src/main/res/drawable
