"""Synthesises the trainer's answer sounds as 48 kHz mono WAVs, the rate Opus encodes at, in the given directory: a bright
rising chime for a right answer and a soft, lower falling knock for a wrong one, each 0.6 s, peaking at -6 dBFS.
Usage: python3 synth.py <dir>"""
import math, struct, sys, wave

RATE = 48000
LENGTH = 0.6
NOTE_LENGTH = 0.5
ATTACK = 0.004


def note(samples, freq, start, partials, decay):
    """Adds one struck note from start seconds on: harmonics as (multiple, level, decay factor) over an exponential fade."""
    first = int(start * RATE)
    for i in range(min(int(NOTE_LENGTH * RATE), len(samples) - first)):
        t = i / RATE
        envelope = min(1, t / ATTACK) * math.exp(-t / decay)
        samples[first + i] += envelope * sum(
            level * math.exp(-t * fade / decay) * math.sin(2 * math.pi * freq * multiple * t) for multiple, level, fade in partials
        )


def render(name, notes):
    samples = [0.0] * int(LENGTH * RATE)
    for args in notes:
        note(samples, *args)
    peak = max(abs(s) for s in samples)
    gain = 10 ** (-6 / 20) / peak
    # A 5 ms fade at the end, so the last sample lands on silence without a click
    tail = int(0.005 * RATE)
    for k in range(tail):
        samples[-1 - k] *= k / tail
    with wave.open(f'{sys.argv[1]}/{name}.wav', 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(s * gain * 32767)) for s in samples))


# A glockenspiel-like bar: the fundamental with a quieter, quicker-fading octave and twelfth
bright = [(1, 1.0, 1.0), (2, 0.35, 1.8), (3, 0.12, 2.6)]
render('right', [(1046.5, 0.0, bright, 0.14), (1318.5, 0.085, bright, 0.18)])

# A muted wooden knock: odd harmonics that die away soon, so it thuds rather than buzzes. Pitched above about 400 Hz, since
# a phone's own speaker plays little below that and would leave only a faint tick of anything lower.
dull = [(1, 1.0, 1.0), (3, 0.28, 1.4), (5, 0.10, 1.8)]
render('wrong', [(659.26, 0.0, dull, 0.11), (493.88, 0.13, dull, 0.15)])
