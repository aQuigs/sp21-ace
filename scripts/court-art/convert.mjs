// Converts Dmitry Fomin's English pattern court cards to the app's court_<rank>_<suit> vector drawables: the figure in its frame,
// recoloured to the card red, navy and saffron. A drawable group can't hold a skew or stretch, so every transform is baked into
// the path data. Usage: node convert.mjs <svg_dir> <drawable_dir>, the SVGs named <rank>_<suit>.svg
import fs from 'node:fs';
import path from 'node:path';
import svgpath from 'svgpath';
import { parse } from 'svgson';

const [inDir, outDir] = process.argv.slice(2);

const RED = '#C8102E';
const NAVY = '#1F3A5F';
const SAFFRON = '#D99A1E';
const INK = '#1B1B1B';

const PAINT = ['fill', 'fill-opacity', 'fill-rule', 'stroke', 'stroke-width', 'stroke-linecap', 'stroke-linejoin', 'stroke-miterlimit', 'stroke-opacity'];

// Fomin paints in white and four colour families, one per colour of the palette
function recolour(colour) {
  if (colour === 'none') return colour;
  if (!/^#[0-9a-f]{6}$/i.test(colour)) throw new Error(`colour ${colour}`);
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(colour.slice(i, i + 2), 16));
  if (r > 240 && g > 240 && b > 240) return colour;
  if (r < 10 && g < 10 && b < 10) return INK;
  if (r > 240 && g > 240) return SAFFRON;
  if (r > 240) return RED;
  if (b > 150) return NAVY;
  throw new Error(`colour ${colour}`);
}

// An inline style outranks the element's own presentation attributes
function withStyle(attributes) {
  const own = { ...attributes };
  for (const declaration of (attributes.style ?? '').split(';')) {
    const [key, value] = declaration.split(':').map((part) => part.trim());
    if (value !== undefined) own[key] = value;
  }
  return own;
}

// As browsers draw it: a missing radius takes the other's, and a zero radius either way leaves the corners square
function rectPath({ x = 0, y = 0, width, height, rx, ry }) {
  [x, y, width, height] = [x, y, width, height].map(Number);
  let [radiusX, radiusY] = [rx ?? ry ?? 0, ry ?? rx ?? 0].map(Number);
  radiusX = Math.min(radiusX, width / 2);
  radiusY = Math.min(radiusY, height / 2);
  const [right, bottom] = [x + width, y + height];
  if (!(radiusX > 0 && radiusY > 0)) return `M${x} ${y}H${right}V${bottom}H${x}Z`;
  const arc = `A${radiusX} ${radiusY} 0 0 1`;
  return `M${x + radiusX} ${y}H${right - radiusX}${arc} ${right} ${y + radiusY}V${bottom - radiusY}${arc} ${right - radiusX} ${bottom}`
    + `H${x + radiusX}${arc} ${x} ${bottom - radiusY}V${y + radiusY}${arc} ${x + radiusX} ${y}Z`;
}

// The determinant of a transform list, to scale stroke widths by the square root of the area it scales by
function determinant(transform) {
  let det = 1;
  for (const [, op, args] of transform.matchAll(/(\w+)\s*\(([^)]*)\)/g)) {
    const n = args.split(/[\s,]+/).filter(Boolean).map(Number);
    if (op === 'matrix') det *= n[0] * n[3] - n[1] * n[2];
    else if (op === 'scale') det *= n[0] * (n.length > 1 ? n[1] : n[0]);
    else if (op !== 'translate' && op !== 'rotate') throw new Error(`transform ${op}`);
  }
  return det;
}

// Bounds from the end and control points, and an arc's radius either way, so a box can only be too big
function bounds(p) {
  const [xs, ys] = [[], []];
  p.iterate(([command, ...n]) => {
    if (command === 'H') xs.push(n[0]);
    else if (command === 'V') ys.push(n[0]);
    else if (command === 'A') {
      const r = Math.max(n[0], n[1]);
      xs.push(n[5] - r, n[5] + r);
      ys.push(n[6] - r, n[6] + r);
    } else {
      for (let i = 0; i < n.length; i += 2) {
        xs.push(n[i]);
        ys.push(n[i + 1]);
      }
    }
  });
  return [Math.min(...xs), Math.min(...ys), Math.max(...xs), Math.max(...ys)];
}

// Fomin's frame runs 30 in from every edge of his 360 by 540 card, open where his index sits, left of 60 and above 150 and
// the same turned about. His blank card and his index are all that lies outside it, and the app's card and index draw those.
function isFigure({ bounds: [left, top, right, bottom] }) {
  const card = right - left > 350 && bottom - top > 530;
  const index = (right <= 60 && bottom <= 150) || (left >= 300 && top >= 390);
  return !card && !index;
}

function argb(colour, alpha) {
  const a = Math.round(Math.max(0, Math.min(1, alpha)) * 255).toString(16).padStart(2, '0');
  return `#${a}${recolour(colour).slice(1)}`.toUpperCase();
}

function paths(svg) {
  const found = [];

  function walk(node, transforms, inherited, opacity) {
    if (node.name === 'defs' || node.name === 'metadata') return;
    const attributes = withStyle(node.attributes);
    const chain = attributes.transform ? [...transforms, attributes.transform] : transforms;
    const style = { ...inherited };
    for (const key of PAINT) if (attributes[key] !== undefined) style[key] = attributes[key];
    const alpha = opacity * parseFloat(attributes.opacity ?? '1');

    const d = node.name === 'path' ? attributes.d : node.name === 'rect' ? rectPath(attributes) : undefined;
    if (d) {
      const transform = chain.join(' ');
      const p = svgpath(d).transform(transform).abs().round(1);
      found.push({ d: p.toString(), bounds: bounds(p), style, alpha, scale: Math.sqrt(Math.abs(determinant(transform))) });
    }
    for (const child of node.children) walk(child, chain, style, alpha);
  }

  walk(svg, [], {}, 1);
  return found;
}

function pathXml({ d, style, alpha, scale }) {
  const out = [`android:pathData="${d}"`];
  const fill = style.fill ?? '#000000';
  if (fill !== 'none') {
    out.push(`android:fillColor="${argb(fill, alpha * parseFloat(style['fill-opacity'] ?? '1'))}"`);
    if (style['fill-rule'] === 'evenodd') out.push('android:fillType="evenOdd"');
  }
  const stroke = style.stroke ?? 'none';
  if (stroke !== 'none') {
    out.push(`android:strokeColor="${argb(stroke, alpha * parseFloat(style['stroke-opacity'] ?? '1'))}"`);
    // A drawable takes a stroke width as a bare number in viewport units, which is what Fomin's px are
    out.push(`android:strokeWidth="${+(parseFloat(style['stroke-width'] ?? '1') * scale).toFixed(2)}"`);
    const cap = style['stroke-linecap'] ?? 'butt';
    const join = style['stroke-linejoin'] ?? 'miter';
    if (cap !== 'butt') out.push(`android:strokeLineCap="${cap}"`);
    if (join !== 'miter') out.push(`android:strokeLineJoin="${join}"`);
    const miter = parseFloat(style['stroke-miterlimit'] ?? '4');
    if (join === 'miter' && miter !== 4) out.push(`android:strokeMiterLimit="${miter}"`);
  }
  return `    <path\n        ${out.join('\n        ')} />`;
}

for (const file of fs.readdirSync(inDir).filter((f) => f.endsWith('.svg')).sort()) {
  const [rank, suit] = path.basename(file, '.svg').split('_');
  const all = paths(await parse(fs.readFileSync(path.join(inDir, file), 'utf8')));
  const figure = all.filter(isFigure);
  // Anything else found outside the frame means the source has changed, and the frame needs looking at again
  if (all.length - figure.length !== 5) throw new Error(`${file}: ${all.length - figure.length} paths outside the frame, not his card and four index paths`);
  // The size a drawable declares only has to stay under lint's 200 dp, since the card draws each figure at its own size
  const xml = `<!-- The ${rank} of ${suit} from Dmitry Fomin's English pattern playing cards (CC0, Wikimedia Commons): his figure in its frame, without his blank card and corner index, recoloured to the card red, navy and saffron by scripts/court-art. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:width="72dp"
    android:height="108dp"
    android:viewportWidth="360"
    android:viewportHeight="540"
    tools:ignore="VectorPath">
${figure.map(pathXml).join('\n')}
</vector>
`;
  fs.writeFileSync(path.join(outDir, `court_${rank}_${suit}.xml`), xml);
}
