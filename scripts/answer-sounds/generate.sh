#!/bin/zsh
# Synthesises the trainer's answer sounds and encodes them as the Opus files in res/raw. Needs python3 and ffmpeg.
set -e
cd "$(dirname "$0")/../.."

WORK=$(mktemp -d)
trap 'rm -rf $WORK' EXIT
python3 scripts/answer-sounds/synth.py $WORK

for name in right wrong; do
    ffmpeg -v error -i $WORK/$name.wav -c:a libopus -b:a 64k -map_metadata -1 -fflags +bitexact -flags:a +bitexact $WORK/${name}_answer.ogg
done
# Both replaced only once both have encoded, so a failure never leaves a new sound beside an old one
mv $WORK/right_answer.ogg $WORK/wrong_answer.ogg app/src/main/res/raw/
