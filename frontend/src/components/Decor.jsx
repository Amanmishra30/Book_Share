import { useMemo } from 'react';

export function Fireflies({ count = 22 }) {
  const flies = useMemo(() => Array.from({ length: count }, (_, i) => ({
    key: i,
    style: {
      '--x': `${Math.round(Math.random() * 100)}%`, '--y': `${Math.round(Math.random() * 100)}%`,
      '--dx': `${Math.round(Math.random() * 120 - 60)}px`, '--dy': `${Math.round(Math.random() * 120 - 60)}px`,
      '--d': `${9 + Math.random() * 10}s`, '--b': `${3 + Math.random() * 4}s`, animationDelay: `${-Math.random() * 8}s`
    }
  })), [count]);
  return <div className="fireflies" aria-hidden="true">{flies.map((f) => <i key={f.key} style={f.style} />)}</div>;
}

/** Hanging ivy, drawn as an SVG, used in the page corners. */
export function Vines({ side = 'left' }) {
  const leaves = [[40, 70, -30], [58, 130, 25], [30, 190, -25], [62, 250, 30], [38, 310, -20], [70, 20, 40], [95, 60, 10], [110, 120, -30]];
  return (
    <svg className={`vines ${side}`} viewBox="0 0 230 360" aria-hidden="true">
      <path d="M0 14 C60 20 90 8 140 22 S210 40 230 24" fill="none" stroke="#6b8f6a" strokeWidth="4" strokeLinecap="round" />
      <path d="M45 16 C38 90 70 150 44 220 C30 270 62 310 48 350" fill="none" stroke="#7da079" strokeWidth="3" strokeLinecap="round" />
      <path d="M105 20 C118 70 90 110 112 160" fill="none" stroke="#7da079" strokeWidth="3" strokeLinecap="round" />
      {leaves.map(([x, y, r], i) => (
        <ellipse key={i} cx={x} cy={y} rx="14" ry="8" transform={`rotate(${r} ${x} ${y})`} fill={i % 3 === 0 ? '#8db38a' : i % 3 === 1 ? '#6b8f6a' : '#a9c1a1'} stroke="#4f7352" strokeWidth="1" />
      ))}
      <circle cx="150" cy="30" r="5" fill="#ebc1b8" /><circle cx="160" cy="36" r="4" fill="#d98f86" /><circle cx="52" cy="200" r="4" fill="#cdc3e4" />
    </svg>
  );
}
