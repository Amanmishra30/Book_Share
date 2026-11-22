import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../api.js';
import { BookCover, Empty, ErrorNote, Loading, Pill } from '../components/ui.jsx';
import { STATUS_TONE, label, money } from '../util.js';

export default function Marketplace() {
  const [q, setQ] = useState('');
  const [scope, setScope] = useState('own');
  const [s, setS] = useState({ data: null, loading: true, error: null });

  useEffect(() => {
    const t = setTimeout(() => {
      get('/sales', { q: q || undefined, scope })
        .then((data) => setS({ data, loading: false, error: null }))
        .catch((e) => setS({ data: null, loading: false, error: e?.response?.data?.error || e.message }));
    }, 250);
    return () => clearTimeout(t);
  }, [q, scope]);

  return (
    <>
      <span className="hand">Books looking for a new home</span>
      <h1 style={{ fontSize: '2.2rem' }}>Marketplace</h1>
      <p className="muted">Demo checkout is available for project testing. It simulates a successful payment and marks the book sold; no real money or payment gateway is involved.</p>
      <div className="card row" style={{ margin: '1rem 0' }}>
        <label className="field grow">Search<input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Title or author" /></label>
        <label className="field">Search in<select value={scope} onChange={(e) => setScope(e.target.value)}><option value="own">My building</option><option value="parent">My neighbourhood</option><option value="root">Whole city</option></select></label>
      </div>
      <ErrorNote msg={s.error} />
      {s.loading ? <Loading /> : s.data.length === 0 ? <Empty icon="🏷️" title="Nothing for sale here yet">Books listed as “sell” or “lend or sell” show up here.</Empty> : (
        <div className="grid">
          {s.data.map((x) => (
            <Link key={x.id} to={`/marketplace/${x.id}`} className="card bookcard">
              <BookCover book={x.book} />
              <div className="bookcard-body">
                <h3>{x.book.title}</h3><span className="muted small">{x.book.author}</span>
                <span className="small">🏷️ {money(x.book.price)} · {x.seller.name}</span>
                <div className="badges"><Pill tone={STATUS_TONE[x.status]}>{label(x.status)}</Pill><Pill tone="sky">{label(x.book.condition)}</Pill></div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
