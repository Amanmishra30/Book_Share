export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : '—');
export const fmtDateTime = (iso) => (iso ? new Date(iso).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—');
export const fmtTime = (iso) => (iso ? new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' }) : '');
export const pct = (r) => (r === null || r === undefined ? null : Math.round(r * 100));
export const label = (s) => (s || '').toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());
export const money = (v) => (v === null || v === undefined ? '' : Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 }));
export const toLocalInput = (d) => { const p = (n) => String(n).padStart(2, '0'); return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`; };

export const CONDITIONS = ['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'WORN'];
export const GENRES = ['Fantasy', 'Classic', 'Fiction', 'Non-fiction', 'Biography', 'Poetry', 'Mystery', 'Romance', 'Science', 'History', 'Children', 'Self-help', 'Textbook'];

export const STATUS_TEXT = {
  PENDING: 'Waiting for the owner', ACCEPTED: 'Accepted · arrange pickup', PICKED_UP: 'Handed over',
  BORROWED: 'On loan', RETURN_DUE: 'Overdue · please return', RETURNED: 'Returned · awaiting confirmation',
  COMPLETED: 'Completed', REJECTED: 'Declined'
};
export const STATUS_TONE = {
  PENDING: 'honey', ACCEPTED: 'sky', PICKED_UP: 'sky', BORROWED: 'moss', RETURN_DUE: 'rose', RETURNED: 'lilac', COMPLETED: 'moss', REJECTED: 'muted',
  LISTED: 'moss', RESERVED: 'honey', SOLD: 'muted', CANCELLED: 'muted'
};
