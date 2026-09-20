#!/bin/zsh
# Synthesises the trainer's answer sounds and encodes them as the Opus files in res/raw. Needs python3 and ffmpeg.
set -e
cd "${0:A:h}"

WAVS=$(mktemp -d)
trap 'rm -rf $WAVS' EXIT
python3 synth.py $WAVS

for name in right wrong; do
    ffmpeg -v error -y -i $WAVS/$name.wav -c:a libopus -b:a 64k -map_metadata -1 -fflags +bitexact -flags:a +bitexact ../../app/src/main/res/raw/${name}_answer.ogg
done
