import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../api.js';
import { BookCard, Empty, ErrorNote, Loading } from '../components/ui.jsx';
import { CONDITIONS, GENRES, label } from '../util.js';

export default function Library() {
  const [f, setF] = useState({ q: '', genre: '', condition: '', listingType: '', available: '', scope: 'own' });
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  useEffect(() => {
    setState((s) => ({ ...s, loading: true }));
    const t = setTimeout(() => {
      const params = Object.fromEntries(Object.entries(f).filter(([, v]) => v !== ''));
      get('/books', params)
        .then((data) => setState({ data, loading: false, error: null }))
        .catch((e) => setState({ data: null, loading: false, error: e?.response?.data?.error || e.message }));
    }, 250);
    return () => clearTimeout(t);
  }, [f]);

  return (
    <>
      <div className="row between">
        <div><span className="hand">The stacks</span><h1 style={{ fontSize: '2.2rem' }}>Community library</h1></div>
        <Link to="/books/new" className="btn honey">+ Shelve a book</Link>
      </div>
      <div className="card" style={{ margin: '1rem 0 1.4rem' }}>
        <div className="filters">
          <label className="field">Search<input value={f.q} onChange={set('q')} placeholder="Title or author" /></label>
          <label className="field">Genre<input list="lib-genres" value={f.genre} onChange={set('genre')} placeholder="Any" /><datalist id="lib-genres">{GENRES.map((g) => <option key={g} value={g} />)}</datalist></label>
          <label className="field">Condition<select value={f.condition} onChange={set('condition')}><option value="">Any</option>{CONDITIONS.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select></label>
          <label className="field">Offer<select value={f.listingType} onChange={set('listingType')}><option value="">Any</option><option value="BORROW">Borrow</option><option value="SALE">Buy</option><option value="BOTH">Both</option></select></label>
          <label className="field">Availability<select value={f.available} onChange={set('available')}><option value="">Any</option><option value="true">Available</option><option value="false">On loan</option></select></label>
          <label className="field">Search in<select value={f.scope} onChange={set('scope')}><option value="own">My building</option><option value="parent">My neighbourhood</option><option value="root">Whole city</option></select></label>
        </div>
      </div>
      <ErrorNote msg={state.error} />
      {state.loading && !state.data ? <Loading /> : state.data?.length === 0
        ? <Empty icon="🌲" title="No books on these shelves yet">Try widening the search to your neighbourhood, or <Link to="/books/new">shelve the first one</Link>.</Empty>
        : <div className="grid">{state.data?.map((b) => <BookCard key={b.id} book={b} />)}</div>}
    </>
  );
}
