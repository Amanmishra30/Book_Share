import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { label, money, pct } from '../util.js';

export const CHECK_SLOTS = ['Front cover', 'Back cover', 'Spine', 'Fanned pages', 'Extra (optional)'];

export function Stars({ value = 0 }) {
  const full = Math.round(value);
  return (
    <span className="stars" aria-label={`${value.toFixed(1)} out of 5`}>
      {[1, 2, 3, 4, 5].map((i) => <span key={i} className={i <= full ? '' : 'off'}>★</span>)}
    </span>
  );
}

export function StarInput({ value, onChange }) {
  return (
    <span className="stars input">
      {[1, 2, 3, 4, 5].map((i) => (
        <button type="button" key={i} className={i <= value ? 'on' : ''} onClick={() => onChange(i)} aria-label={`${i} star${i > 1 ? 's' : ''}`}>★</button>
      ))}
    </span>
  );
}

export const Pill = ({ tone = '', children }) => <span className={`pill ${tone}`}>{children}</span>;
export const Loading = () => <div className="spinner" role="status" aria-label="Loading" />;
export const ErrorNote = ({ msg }) => (msg ? <div className="note err" role="alert">{msg}</div> : null);
export const Empty = ({ icon = '🍃', title, children }) => (
  <div className="empty"><div className="big">{icon}</div><h3>{title}</h3><div>{children}</div></div>
);

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="row between"><h3>{title}</h3><button className="btn ghost small" onClick={onClose}>Close</button></div>
        {children}
      </div>
    </div>
  );
}

const PAL = [['#d6e8cf', '#97b793'], ['#f6dfb8', '#dcab6c'], ['#ecd3da', '#c897a8'], ['#d3dcf0', '#93a4cf'], ['#f2e6b0', '#d1b862'], ['#d4e8e6', '#8bb9b4']];
const hash = (s) => [...(s || '')].reduce((a, c) => (a * 31 + c.charCodeAt(0)) >>> 0, 7);

export function BookCover({ book }) {
  if (book.coverUrl) return <div className="cover"><img src={book.coverUrl} alt={`Cover of ${book.title}`} loading="lazy" /></div>;
  const [a, b] = PAL[hash(book.title) % PAL.length];
  return (
    <div className="cover gen" style={{ background: `linear-gradient(160deg, ${a}, ${b})` }}>
      <em aria-hidden="true">🌿</em><b>{book.title}</b><span>{book.author}</span>
    </div>
  );
}

export function BookBadges({ book }) {
  return (
    <div className="badges">
      <Pill tone="sky">{label(book.condition)}</Pill>
      {book.listingType !== 'SALE' && <Pill tone={book.available ? 'moss' : 'muted'}>{book.available ? 'Can borrow' : 'On loan'}</Pill>}
      {book.listingType !== 'BORROW' && book.price != null && <Pill tone="honey">For sale · {money(book.price)}</Pill>}
    </div>
  );
}

export function BookCard({ book }) {
  return (
    <Link to={`/books/${book.id}`} className="card bookcard">
      <BookCover book={book} />
      <div className="bookcard-body">
        <h3>{book.title}</h3>
        <span className="muted small">{book.author}</span>
        <span className="small">📍 {book.communityName} · {book.owner?.name}</span>
        <BookBadges book={book} />
      </div>
    </Link>
  );
}

export function PhotoGrid({ urls = [] }) {
  const [big, setBig] = useState(null);
  return (
    <>
      <div className="photos">{urls.map((u, i) => <img key={u + i} src={u} alt={`Condition photo ${i + 1}`} loading="lazy" onClick={() => setBig(u)} />)}</div>
      {big && <div className="lightbox" onClick={() => setBig(null)}><img src={big} alt="Enlarged condition photo" /></div>}
    </>
  );
}

/** Five labelled photo slots (front, back, spine, fanned pages, optional extra). `files` is an array of File|null. */
export function PhotoPicker({ files, onChange }) {
  const urls = useMemo(() => files.map((f) => (f ? URL.createObjectURL(f) : null)), [files]);
  useEffect(() => () => urls.forEach((u) => u && URL.revokeObjectURL(u)), [urls]);
  const set = (i, f) => onChange(files.map((x, k) => (k === i ? f : x)));
  return (
    <div className="slots">
      {CHECK_SLOTS.map((name, i) => (
        <label key={name} className={`slot ${files[i] ? 'filled' : ''}`}>
          {urls[i] ? <img src={urls[i]} alt={name} /> : <span aria-hidden="true" style={{ fontSize: '1.8rem' }}>📷</span>}
          <span>{name}</span>
          <input type="file" accept="image/*" onChange={(e) => e.target.files[0] && set(i, e.target.files[0])} />
          {files[i] && <button type="button" aria-label={`Remove ${name}`} onClick={(e) => { e.preventDefault(); set(i, null); }}>✕</button>}
        </label>
      ))}
    </div>
  );
}
export const emptySlots = () => [null, null, null, null, null];
export const pickedFiles = (slots) => slots.filter(Boolean);

/** Star rating + the SEPARATE compliance numbers, always with raw counts so small samples are not misleading. */
export function TrustCard({ title, score, ratingCount, compliance }) {
  const c = compliance || {};
  return (
    <div className="trust">
      {title && <b>{title}</b>}
      {score !== undefined && (
        <div className="line"><span>Star rating</span>
          <b>{ratingCount > 0 ? <><Stars value={score} /> {score.toFixed(1)} ({ratingCount})</> : 'No ratings yet'}</b></div>
      )}
      <div className="line"><span>On-time returns</span>
        <b>{c.totalBorrows > 0 ? `${c.onTimeReturns}/${c.totalBorrows} on time (${pct(c.onTimeRatio)}%)` : 'No borrows yet'}</b></div>
      <div className="line"><span>Completed lends</span>
        <b>{c.acceptedLends > 0 ? `${c.completedLends}/${c.acceptedLends} (${pct(c.lendRatio)}%)` : 'No lends yet'}</b></div>
    </div>
  );
}

export const UserLink = ({ u }) => (u ? <Link to={`/profile/${u.id}`}>{u.name}</Link> : null);
