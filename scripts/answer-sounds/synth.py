"""Synthesises the trainer's answer sounds as 48 kHz mono WAVs, the rate Opus encodes at, in the given directory: right.wav and
wrong.wav, each 0.6 s and as loud as the other on a phone's own speaker. Usage: python3 synth.py <dir>"""
import math, random, statistics, struct, sys, wave

RATE = 48000
LENGTH = 0.6
PEAK = 10 ** (-1 / 20)
LIMIT_DB = 3
RELEASE = 0.003

C6 = 1046.5
# A clay chip clacks from about 3 to 9 kHz and dies within tens of milliseconds; the stack under it adds a short, lower knock.
# Each mode is (frequency, seconds to fade 60 dB, gain).
CLAY = [(3200, 0.050, 1.0), (4860, 0.038, 0.7), (6940, 0.026, 0.45), (9280, 0.018, 0.25), (1150, 0.030, 0.35)]


def bar(freq, ring):
    """A marimba bar's modes, with overtones at about 4 and 9 times its note, near the 1 : 4 : 10 its bars are cut to, ringing
    for `ring` s."""
    return [(freq, ring, 1.0), (freq * 3.93, ring * 0.2, 0.22), (freq * 9.2, ring * 0.07, 0.06)]


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


def impact(out, at, amp, modes, contact, grit, rng, pitch):
    """One hit at `at` s: a contact pulse `contact` s wide, brighter the shorter, with `grit` of rough noise, rung through the
    modes. The pulse's area is amp, so a softer contact is as strong but duller."""
    width = max(2, round(contact * RATE))
    top = amp * math.pi / (2 * width)
    excitation = [
        (top * math.sin(math.pi * k / width) if k < width else 0.0) + grit * top * rng.gauss(0, 1) * math.exp(-k / (0.0006 * RATE))
        for k in range(max(width, int(0.002 * RATE)))
    ]
    start = int(at * RATE)
    # Where a hit lands decides how strongly each mode rings, and which way it first swings. Modes that all start the same way
    # would add up to a spike no real hit makes, which would cap how loud the sound could be.
    for freq, t60, gain in modes:
        resonate(excitation, freq * pitch, t60, out, start, gain * rng.uniform(0.7, 1.3) * rng.choice((-1, 1)))


def right(out, rng):
    """One marimba note, C6, struck with a soft mallet and left to ring."""
    impact(out, 0.0, 1.0, bar(C6, 0.45), 0.0006, 0.05, rng, 1.0)


def wrong(out, rng):
    """Two clay chips set down, each landing with a clack and a quick rattle as it settles. A little under a clay chip's own pitch,
    landing softly and settling slowly, so it doesn't sound upbeat."""
    for at, amp in [(0.0, 1.0), (0.17, 0.8)]:
        pitch = 0.88 * (1 + rng.uniform(-0.04, 0.04))
        gap = 0.042 * 1.35
        for _ in range(3):
            impact(out, at, amp, CLAY, 0.00025, 0.3, rng, pitch * (1 + rng.uniform(-0.012, 0.012)))
            at, gap, amp = at + gap, gap * 0.62, amp * 0.45


def limited(samples):
    """Brought down by up to LIMIT_DB where it peaks, then peaking at 1, for a little more loudness at the same peak. The gain
    looks a millisecond ahead and recovers over RELEASE, so it turns each hit's first few milliseconds down without clipping the
    wave. Only a little, as every peak above the limit comes out level with the rest: much more and a chip's settling rattle
    would be as loud as its landing."""
    ahead = RATE // 1000
    ceiling = max(abs(s) for s in samples) * 10 ** (-LIMIT_DB / 20)
    need = [min(1, ceiling / max(abs(s), 1e-12)) for s in samples]
    lowest = [min(need[n:n + ahead + 1]) for n in range(len(samples))]
    back = 1 - math.exp(-1 / (RELEASE * RATE))
    gain, shaped = 1.0, []
    for n, s in enumerate(samples):
        gain = min(statistics.fmean(lowest[max(0, n - ahead):n + 1]), gain + (1 - gain) * back)
        shaped.append(s * gain)
    top = max(abs(s) for s in shaped)
    return [s / top for s in shaped]


def loudness(samples):
    """The loudest 400 ms RMS, as momentary loudness is measured, through a steep bass cut (13 dB down at 400 Hz, 3.5 dB at
    1 kHz), roughly as a phone's own speaker gives it out."""
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
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(s * gain * 32767)) for s in samples))


def render(directory, sounds):
    """Writes each of sounds, a {name: make} map, to <directory>/<name>.wav at one loudness: the most that keeps every one
    within PEAK. Each has a seed of its own, so retuning one never changes another's noise or hits."""
    shaped, levels = {}, {}
    for name, make in sounds.items():
        samples = [0.0] * int(LENGTH * RATE)
        make(samples, random.Random(name))
        shaped[name] = limited(samples)
        levels[name] = loudness(shaped[name])
    target = PEAK * min(levels.values())
    for name, samples in shaped.items():
        write(f'{directory}/{name}.wav', samples, target / levels[name])


if __name__ == '__main__':
    render(sys.argv[1], {'right': right, 'wrong': wrong})
