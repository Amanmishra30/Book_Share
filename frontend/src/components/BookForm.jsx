import { useState } from 'react';
import { CONDITIONS, GENRES, label } from '../util.js';
import { emptySlots, ErrorNote, PhotoPicker, pickedFiles } from './ui.jsx';

/** Shared by "Add a book" and "Edit book". Photos (listing condition check) are optional but must be 3 to 5 when given. */
export default function BookForm({ initial, onSubmit, submitLabel = 'Shelve this book', withPhotos = true, showAvailability = false, busy }) {
  const [f, setF] = useState({
    title: '', author: '', genre: '', edition: '', condition: 'GOOD', listingType: 'BORROW', price: '',
    lendingDurationDays: 14, description: '', available: true, ...(initial || {})
  });
  const [cover, setCover] = useState(null);
  const [slots, setSlots] = useState(emptySlots());
  const [error, setError] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const forSale = f.listingType !== 'BORROW';

  const submit = (e) => {
    e.preventDefault();
    const photos = pickedFiles(slots);
    if (withPhotos && photos.length > 0 && photos.length < 3) return setError('Add at least 3 condition photos (front, back, spine, fanned pages) or none at all.');
    if (forSale && (f.price === '' || Number(f.price) < 0)) return setError('Please set a price for a book you want to sell.');
    setError('');
    onSubmit({
      book: {
        title: f.title, author: f.author, genre: f.genre || null, edition: f.edition || null, condition: f.condition,
        description: f.description || null, listingType: f.listingType, price: forSale ? Number(f.price) : null,
        lendingDurationDays: Number(f.lendingDurationDays) || 14, available: showAvailability ? f.available : null
      },
      cover, photos
    });
  };

  return (
    <form onSubmit={submit} className="stack">
      <div className="form-grid">
        <label className="field">Title<input required value={f.title} onChange={set('title')} maxLength={200} /></label>
        <label className="field">Author<input required value={f.author} onChange={set('author')} maxLength={200} /></label>
        <label className="field">Genre<input list="genres" value={f.genre || ''} onChange={set('genre')} /><datalist id="genres">{GENRES.map((g) => <option key={g} value={g} />)}</datalist></label>
        <label className="field">Edition<input value={f.edition || ''} onChange={set('edition')} placeholder="e.g. 2nd, paperback" /></label>
        <label className="field">Condition
          <select value={f.condition} onChange={set('condition')}>{CONDITIONS.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select></label>
        <label className="field">What would you like to do with it?
          <select value={f.listingType} onChange={set('listingType')}>
            <option value="BORROW">Lend it</option><option value="SALE">Sell it</option><option value="BOTH">Lend or sell</option></select></label>
        {forSale && <label className="field">Price<input type="number" min="0" step="0.01" value={f.price ?? ''} onChange={set('price')} /></label>}
        {f.listingType !== 'SALE' && <label className="field">Lending duration (days)<input type="number" min="1" max="120" value={f.lendingDurationDays} onChange={set('lendingDurationDays')} /></label>}
        <label className="field full">Description<textarea value={f.description || ''} onChange={set('description')} maxLength={2000} placeholder="A few words about the book and its little scars…" /></label>
        <label className="field full">Cover photo (optional)<input type="file" accept="image/*" onChange={(e) => setCover(e.target.files[0] || null)} /></label>
        {showAvailability && <label className="row"><input type="checkbox" checked={!!f.available} onChange={set('available')} /> Available to borrow</label>}
      </div>
      {withPhotos && (
        <div>
          <h3>Condition photos</h3>
          <p className="muted small">A quick record of how the book looks today: front cover, back cover, spine and a fanned-page shot (3–5 photos). It protects both of you later.</p>
          <PhotoPicker files={slots} onChange={setSlots} />
        </div>
      )}
      <ErrorNote msg={error} />
      <div><button className="btn" disabled={busy}>{busy ? 'Shelving…' : submitLabel}</button></div>
    </form>
  );
}
