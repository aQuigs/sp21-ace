"""Synthesises the trainer's answer sounds as 44.1 kHz mono WAVs in the given directory: a bright rising chime for a right
answer and a soft, low falling knock for a wrong one, each 0.6 s, peaking at -6 dBFS. Usage: python3 synth.py <dir>"""
import math, struct, sys, wave

RATE = 44100


def note(freq, start, length, partials, decay, attack=0.004):
    """(sample index, value) pairs of one struck note: harmonics as (multiple, level, decay factor) over an exponential fade."""
    out = []
    for i in range(int(length * RATE)):
        t = i / RATE
        envelope = min(1, t / attack) * math.exp(-t / decay)
        value = sum(level * math.exp(-t * fade / decay) * math.sin(2 * math.pi * freq * multiple * t) for multiple, level, fade in partials)
        out.append((int(start * RATE) + i, envelope * value))
    return out


def render(name, notes, length):
    samples = [0.0] * int(length * RATE)
    for n in notes:
        for i, v in n:
            if i < len(samples):
                samples[i] += v
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
render('right', [note(1046.5, 0.0, 0.5, bright, 0.14), note(1318.5, 0.085, 0.5, bright, 0.18)], 0.6)

# A muted wooden knock: odd harmonics that die away fast, so it thuds rather than buzzes
dull = [(1, 1.0, 1.0), (3, 0.28, 2.5), (5, 0.08, 4.0)]
render('wrong', [note(220.0, 0.0, 0.5, dull, 0.11), note(164.8, 0.13, 0.5, dull, 0.15)], 0.6)
