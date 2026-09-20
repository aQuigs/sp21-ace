"""Synthesises the trainer's answer sounds as 48 kHz mono WAVs, the rate Opus encodes at, in the given directory. They come
from the card table rather than a chime: for a right answer two clay chips paid onto your stack, and for a wrong one your
chips raked away across the felt. Each is 0.6 s, and the two are as loud as each other on a phone's own speaker.
Usage: python3 synth.py <dir>"""
import math, random, struct, sys, wave

RATE = 48000
LENGTH = 0.6
PEAK = 10 ** (-1 / 20)
# How far the limiter may turn a hit's first spike down, and how soon it lets go
CEILING = 9
RELEASE = 0.003

# A clay chip clacks in the 3 to 7 kHz range and dies within tens of milliseconds; the stack under it adds a short, lower knock.
# Each mode is (frequency, seconds to fade 60 dB, gain).
CLAY = [(3200, 0.050, 1.0), (4860, 0.038, 0.7), (6940, 0.026, 0.45), (9280, 0.018, 0.25), (1150, 0.030, 0.35)]
# Chips dragged in a heap knock softly, lower and deader than one dropped
MUFFLED_CLAY = [(f * 0.8, t60 * 0.5, gain) for f, t60, gain in CLAY]


def resonate(excitation, freq, t60, out, start, gain):
    """Rings a two-pole resonator at freq Hz, fading 60 dB over t60 s, from the excitation, into out from sample start."""
    r = 10 ** (-3 / (t60 * RATE))
    w = 2 * math.pi * freq / RATE
    a1, a2 = 2 * r * math.cos(w), -r * r
    norm = gain * math.sin(w)
    y1 = y2 = 0.0
    for n in range(min(int(t60 * RATE), len(out) - start)):
        x = excitation[n] if n < len(excitation) else 0.0
        y = x + a1 * y1 + a2 * y2
        out[start + n] += norm * y
        y2, y1 = y1, y


def impact(out, at, amp, modes, contact, grit, rng, pitch=1.0):
    """One hit at `at` s: a contact pulse `contact` s wide, brighter the shorter, with `grit` of rough noise, rung through the
    modes. The pulse's area is amp, so a softer contact is as strong but duller."""
    width = max(2, round(contact * RATE))
    top = amp * math.pi / (2 * width)
    excitation = [
        (top * math.sin(math.pi * k / width) if k < width else 0.0) + grit * top * rng.gauss(0, 1) * math.exp(-k / (0.0006 * RATE))
        for k in range(max(width, int(0.002 * RATE)))
    ]
    # Where one thing strikes another decides which of its modes ring out, so no two hits sound quite alike
    for freq, t60, gain in modes:
        resonate(excitation, freq * pitch, t60, out, int(at * RATE), gain * rng.uniform(0.7, 1.3))


def swish(out, at, length, band_from, band_to, q, level, rng):
    """Felt brushed for `length` s: noise through a band gliding from band_from to band_to Hz, swelling then fading, with the
    uneven drag of the cloth."""
    start, n = int(at * RATE), int(length * RATE)
    low = band = drag = 0.0
    for k in range(min(n, len(out) - start)):
        t = k / n
        g = 2 * math.sin(math.pi * band_from * (band_to / band_from) ** t / RATE)
        high = rng.gauss(0, 1) - low - band / q
        band += g * high
        low += g * band
        drag += 0.004 * (rng.gauss(0, 1) - drag)
        envelope = math.sin(math.pi / 2 * min(1, t / 0.15)) ** 2 * (1 - t) ** 1.5
        out[start + k] += level * envelope * (1 + 8 * drag) * band


def right(out, rng):
    """Two chips paid onto your stack, each landing with a clack and a quick rattle, every gap and hit smaller than the last."""
    for at, amp in [(0.0, 1.0), (0.13, 0.8)]:
        pitch = 1 + rng.uniform(-0.04, 0.04)
        chip = [(f * pitch, t60, gain) for f, t60, gain in CLAY]
        gap = 0.042
        while gap >= 0.012:
            impact(out, at, amp, chip, 0.00012, 0.3, rng, 1 + rng.uniform(-0.012, 0.012))
            at, gap, amp = at + gap, gap * 0.62, amp * 0.45


def wrong(out, rng):
    """Your chips raked away across the felt, knocking together as they go."""
    swish(out, 0.0, 0.48, 2400, 800, 1.4, 0.05, rng)
    for at, amp in [(0.04, 0.45), (0.13, 0.3), (0.23, 0.2)]:
        impact(out, at, amp, MUFFLED_CLAY, 0.0005, 0.4, rng, 1 + rng.uniform(-0.05, 0.05))


def limited(samples):
    """Limited to CEILING dB below its peak, then peaking at 1, so the first millisecond of each hit, which the ear barely hears
    as loudness, no longer caps how loud the rest can be. The gain looks a millisecond ahead and eases back over RELEASE, so it
    turns each spike down without bending the wave the way clipping would."""
    ahead = RATE // 1000
    ceiling = max(abs(s) for s in samples) * 10 ** (-CEILING / 20)
    need = [min(1, ceiling / max(abs(s), 1e-12)) for s in samples] + [1] * ahead
    lowest = [min(need[n:n + ahead + 1]) for n in range(len(samples))]
    back = 1 - math.exp(-1 / (RELEASE * RATE))
    gain, shaped = 1.0, []
    for n, s in enumerate(samples):
        ramp = sum(lowest[max(0, n - ahead):n + 1]) / (n + 1 - max(0, n - ahead))
        gain = min(ramp, gain + (1 - gain) * back)
        shaped.append(s * gain)
    top = max(abs(s) for s in shaped)
    return [s / top for s in shaped]


def loudness(samples):
    """The loudest 400 ms RMS, as momentary loudness is measured, through a 400 Hz bass cut, roughly as a phone's own speaker
    gives it out."""
    a = math.exp(-2 * math.pi * 400 / RATE)
    x = list(samples)
    for _ in range(4):
        prev_in = prev_out = 0.0
        for i, s in enumerate(x):
            prev_out = a * (prev_out + s - prev_in)
            prev_in, x[i] = s, prev_out
    window = int(0.4 * RATE)
    return max(math.sqrt(sum(v * v for v in x[i:i + window]) / window) for i in range(0, len(x) - window + 1, RATE // 100))


def write(path, samples, gain):
    # A 5 ms fade at the end, so the last sample lands on silence without a click
    tail = int(0.005 * RATE)
    for k in range(tail):
        samples[-1 - k] *= k / tail
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(s * gain * 32767)) for s in samples))


# Seeded, so the same script always makes the same sounds
rng = random.Random('chips')
sounds = {}
for name, make in [('right', right), ('wrong', wrong)]:
    samples = [0.0] * int(LENGTH * RATE)
    make(samples, rng)
    sounds[name] = limited(samples)
levels = {name: loudness(samples) for name, samples in sounds.items()}
# As loud as the louder-peaking one allows, the other matched to it
target = min(PEAK * level for level in levels.values())
for name, samples in sounds.items():
    write(f'{sys.argv[1]}/{name}.wav', samples, target / levels[name])
