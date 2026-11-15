import fs from 'fs';
import path from 'path';
import { pathToFileURL } from 'url';

const [pkg, root] = process.argv.slice(2);
const MAP = {
  home: 'house', rec: 'notebook-pen', cal: 'calendar', set: 'sliders-horizontal', back: 'chevron-left', chev: 'chevron-right',
  check: 'check', drop: 'droplet', plus: 'plus', minus: 'minus', close: 'x', COMFORT: 'thermometer', QUIET: 'moon', RESET: 'wind', REPLAY: 'rotate-ccw', PLACE: 'map-pin', REST: 'coffee', MUSIC: 'music',
};
const PROPS = ['scale', 'scaleX', 'scaleY', 'rotate', 'x', 'y', 'opacity', 'strokeDashoffset', 'pathLength'];
const EASE = { linear: [0, 0, 1, 1], easeIn: [.42, 0, 1, 1], easeOut: [0, 0, .58, 1], easeInOut: [.42, 0, .58, 1], circOut: [0, .55, .45, 1], backOut: [.33, 1.53, .69, .99] };

const tmp = fs.mkdtempSync(path.join(path.dirname(pkg), 'ai-'));
fs.writeFileSync(`${tmp}/react.mjs`, 'export const forwardRef=f=>f,useCallback=f=>f,useImperativeHandle=()=>{},useRef=v=>({current:v}),useEffect=()=>{},useState=v=>[v,()=>{}],useMemo=f=>f();');
fs.writeFileSync(`${tmp}/jsx.mjs`, 'export const jsx=(type,props)=>({type,props}),jsxs=jsx,Fragment="frag";');
fs.writeFileSync(`${tmp}/motion.mjs`, 'export const a=(...c)=>c.filter(Boolean).join(" "),b="LazyMotion",c={},d=()=>false,e=()=>({start(){}}),f=new Proxy({},{get:(_,k)=>"motion."+k});');

const shape = (tag, p) => tag === 'path' ? p.d
  : tag === 'circle' ? `M${+p.cx - +p.r} ${p.cy}a${p.r} ${p.r} 0 1 0 ${2 * p.r} 0a${p.r} ${p.r} 0 1 0 ${-2 * p.r} 0`
  : tag === 'line' ? `M${p.x1} ${p.y1}L${p.x2} ${p.y2}`
  : tag === 'polyline' ? 'M' + p.points.trim().split(/\s+/).join('L')
  : tag === 'rect' ? rect(+p.x || 0, +p.y || 0, +p.width, +p.height, +(p.rx ?? p.ry ?? 0))
  : null;
const rect = (x, y, w, h, r) => r ? `M${x + r} ${y}h${w - 2 * r}a${r} ${r} 0 0 1 ${r} ${r}v${h - 2 * r}a${r} ${r} 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a${r} ${r} 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a${r} ${r} 0 0 1 ${r} ${-r}z` : `M${x} ${y}h${w}v${h}h${-w}z`;

const tracksOf = (props) => {
  const v = props.variants; if (!v) return [];
  const normal = v.normal || {};
  let anim = v.animate; if (typeof anim === 'function') anim = anim(props.custom);
  if (!anim) return [];
  const tr = anim.transition || {};
  return PROPS.filter(k => k in anim).map(k => {
    const t = { ...tr, ...(tr[k] || {}) };
    let values = [].concat(anim[k]); if (values.length === 1) values = [normal[k] ?? values[0], values[0]];
    const times = t.times || values.map((_, i) => i / (values.length - 1));
    const e = Array.isArray(t.ease) && typeof t.ease[0] === 'number' ? t.ease : EASE[Array.isArray(t.ease) ? t.ease[0] : t.ease] || EASE.easeInOut;
    return { prop: k, values: values.map(Number), times, dur: t.type === 'spring' ? .6 : t.duration ?? .8, delay: t.delay ?? 0, ease: e, rest: Number(normal[k] ?? values.at(-1)) };
  });
};

const nodeOf = t => {
  if (!t || typeof t !== 'object') return null;
  if (t.type === 'LazyMotion' || t.type === 'motion.div' || t.type === 'motion.svg') return [].concat(t.props.children ?? []).map(nodeOf).flat().filter(Boolean);
  const tag = String(t.type).replace('motion.', ''), p = t.props || {};
  const view = p.style?.transformBox === 'view-box';
  const n = { d: shape(tag, p), tracks: tracksOf(p), origin: view ? [parseFloat(p.style.originX ?? 12), parseFloat(p.style.originY ?? 12)] : null,
    dash: p.strokeDasharray ? String(p.strokeDasharray).split(/[\s,]+/).map(Number) : null, kids: [] };
  if (tag === 'g') n.kids = [].concat(p.children ?? []).map(nodeOf).flat().filter(Boolean);
  else if (!n.d) { console.warn('skip', tag); return null; }
  return n;
};

const icons = {};
for (const [key, name] of Object.entries(MAP)) {
  const chunk = fs.readFileSync(`${pkg}/dist/icons/lucide/${name}-icon.js`, 'utf8').match(/chunk-[A-Z0-9]+\.js/)[0];
  const code = fs.readFileSync(`${pkg}/dist/${chunk}`, 'utf8').replace(/from"\.\/chunk-VLGC2A5L\.js"/, 'from"./motion.mjs"').replace(/from"react\/jsx-runtime"/, 'from"./jsx.mjs"').replace(/from"react"/, 'from"./react.mjs"');
  fs.writeFileSync(`${tmp}/${name}.mjs`, code);
  const C = Object.values(await import(pathToFileURL(`${tmp}/${name}.mjs`)))[0];
  icons[key] = nodeOf(C({}, null));
}
fs.rmSync(tmp, { recursive: true });

let css = '.ai *{transform-box:fill-box;transform-origin:center}\n', k = 0;
const cssVal = (prop, v) => ({ scale: `scale:${v}`, scaleX: `scale:${v} 1`, scaleY: `scale:1 ${v}`, rotate: `rotate:${v}deg`, x: `translate:${v}px 0`, y: `translate:0 ${v}px`,
  opacity: `opacity:${v}`, strokeDashoffset: `stroke-dashoffset:${v}`, pathLength: `stroke-dashoffset:${1 - v}` })[prop];
const svgOf = n => {
  const cls = n.tracks.length ? `k${k++}` : '';
  const attrs = [cls && `class="${cls}"`, n.origin && `style="transform-box:view-box;transform-origin:${n.origin[0]}px ${n.origin[1]}px"`,
    n.dash && `stroke-dasharray="${n.dash.join(' ')}"`, n.tracks.some(t => t.prop === 'pathLength') && 'pathLength="1" stroke-dasharray="1 1"'].filter(Boolean).join(' ');
  if (cls) {
    const anims = n.tracks.map((t, i) => {
      const name = `ai${cls}_${i}`;
      css += `@keyframes ${name}{${t.times.map((s, j) => `${+(s * 100).toFixed(2)}%{${cssVal(t.prop, t.values[j])}}`).join('')}}\n`;
      return `${name} ${t.dur}s cubic-bezier(${t.ease.join(',')}) ${t.delay}s both`;
    });
    css += `.ai.play .${cls}{animation:${anims.join(',')}}\n`;
  }
  return n.kids.length ? `<g ${attrs}>${n.kids.map(svgOf).join('')}</g>` : `<path ${attrs} d="${n.d}"/>`;
};
const js = Object.entries(icons).map(([key, ns]) => `  ${key}: '${ns.map(svgOf).join('')}',`).join('\n');
fs.writeFileSync(`${root}/design/icons.js`,
  `// AnimateIcons (MIT) / Lucide (ISC), see design/tools/ICONS-NOTICE.md\nconst AI_CSS = \`${css}\`;\nconst AI = {\n${js}\n};\n`);

const f = x => `${+x.toFixed(4)}f`, fa = a => `floatArrayOf(${a.map(f).join(', ')})`;
const kt = n => `Node(${n.d ? JSON.stringify(n.d) : 'null'}, listOf(${n.tracks.map(t => `Track(P.${t.prop}, ${fa(t.values)}, ${fa(t.times)}, ${f(t.dur)}, ${f(t.delay)}, CubicBezierEasing(${t.ease.map(f).join(', ')}), ${f(t.rest)})`).join(', ')}), ${n.origin ? `Offset(${f(n.origin[0])}, ${f(n.origin[1])})` : 'null'}, ${n.dash ? fa(n.dash) : 'null'}, listOf(${n.kids.map(kt).join(', ')}))`;
const care = ['COMFORT', 'QUIET', 'RESET', 'REPLAY', 'PLACE', 'REST', 'MUSIC'];
const kotlin = `// AnimateIcons (MIT) / Lucide (ISC), see design/tools/ICONS-NOTICE.md
package com.onme.app

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset

object Ic {
${Object.entries(icons).filter(([key]) => !care.includes(key)).map(([key, ns]) => `    val ${key.toUpperCase()} = AnimIcon(listOf(${ns.map(kt).join(', ')}))`).join('\n')}
    val CARE = mapOf(
${care.map(c => `        com.onme.core.CareId.${c} to AnimIcon(listOf(${icons[c].map(kt).join(', ')})),`).join('\n')}
    )
}
`;
fs.writeFileSync(`${root}/app/src/main/kotlin/com/onme/app/Icons.kt`, kotlin);
console.log('icons', Object.keys(icons).length);
