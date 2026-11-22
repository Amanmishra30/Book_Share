import { useState } from 'react';
import { Link } from 'react-router-dom';
import { del, errMsg, get, multipart, post } from '../api.js';
import { useLoad } from '../hooks.js';
import BookForm from '../components/BookForm.jsx';
import { useToast } from '../components/Toast.jsx';
import { BookBadges, BookCover, Empty, ErrorNote, Loading, Modal, PhotoPicker, emptySlots, pickedFiles } from '../components/ui.jsx';

export default function MyShelf() {
  const toast = useToast();
  const shelf = useLoad(() => get('/books/mine'), []);
  const [edit, setEdit] = useState(null);
  const [photosFor, setPhotosFor] = useState(null);
  const [slots, setSlots] = useState(emptySlots());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const run = async (fn, ok) => {
    setBusy(true); setError('');
    try { await fn(); if (ok) toast.ok(ok); await shelf.reload(); return true; } catch (e) { setError(errMsg(e)); toast.err(errMsg(e)); return false; } finally { setBusy(false); }
  };

  const saveEdit = ({ book, cover }) => run(async () => { await multipart('put', `/books/${edit.id}`, { json: { book }, files: { cover } }); setEdit(null); }, 'Book updated.');
  const savePhotos = () => {
    const files = pickedFiles(slots);
    if (files.length < 3) return setError('Please add 3 to 5 photos.');
    return run(async () => { await multipart('post', `/books/${photosFor.id}/condition-checks`, { files: { photos: files } }); setPhotosFor(null); setSlots(emptySlots()); }, 'Condition photos saved.');
  };

  return (
    <>
      <div className="row between">
        <div><span className="hand">Your corner of the library</span><h1 style={{ fontSize: '2.2rem' }}>My shelf</h1></div>
        <Link to="/books/new" className="btn honey">+ Shelve a book</Link>
      </div>
      <ErrorNote msg={shelf.error || (!edit && !photosFor ? error : '')} />
      {shelf.loading ? <Loading /> : shelf.data?.length === 0
        ? <Empty icon="📚" title="Your shelf is empty">Add the first book you are happy to share.</Empty>
        : (
          <div className="stack">
            {shelf.data.map((b) => (
              <div key={b.id} className="card req-card">
                <Link to={`/books/${b.id}`}><BookCover book={b} /></Link>
                <div className="stack gap-s">
                  <div><Link to={`/books/${b.id}`}><b style={{ fontFamily: 'var(--serif)', fontSize: '1.15rem' }}>{b.title}</b></Link><div className="muted small">{b.author}</div></div>
                  <BookBadges book={b} />
                  {b.saleListingId && <Link className="small" to={`/marketplace/${b.saleListingId}`}>View sale listing ({b.saleStatus?.toLowerCase()})</Link>}
                </div>
                <div className="actions row" style={{ justifyContent: 'flex-end' }}>
                  <button className="btn ghost small" onClick={() => { setError(''); setEdit(b); }}>Edit</button>
                  <button className="btn ghost small" onClick={() => { setError(''); setPhotosFor(b); }}>Condition photos</button>
                  {b.listingType !== 'BORROW' && !b.saleListingId && (
                    <button className="btn honey small" disabled={busy} onClick={() => run(() => post('/sales', { bookId: b.id }), 'Listed for sale.')}>List for sale</button>
                  )}
                  <button className="btn danger small" disabled={busy} onClick={() => window.confirm(`Take “${b.title}” off the shelf?`) && run(() => del(`/books/${b.id}`), 'Removed from the shelf.')}>Remove</button>
                </div>
              </div>
            ))}
          </div>
        )}

      {edit && (
        <Modal title={`Edit “${edit.title}”`} onClose={() => setEdit(null)}>
          <BookForm initial={{ title: edit.title, author: edit.author, genre: edit.genre || '', edition: edit.edition || '', condition: edit.condition, listingType: edit.listingType, price: edit.price ?? '', lendingDurationDays: edit.lendingDurationDays, description: edit.description || '', available: edit.available }}
            withPhotos={false} showAvailability submitLabel="Save changes" busy={busy} onSubmit={saveEdit} />
          <ErrorNote msg={error} />
        </Modal>
      )}
      {photosFor && (
        <Modal title={`Condition photos for “${photosFor.title}”`} onClose={() => { setPhotosFor(null); setSlots(emptySlots()); }}>
          <p className="muted small">Front cover, back cover, spine and a fanned-page shot. A new set is added to the book’s history; older sets stay on record.</p>
          <PhotoPicker files={slots} onChange={setSlots} />
          <ErrorNote msg={error} />
          <div style={{ marginTop: '1rem' }}><button className="btn" disabled={busy} onClick={savePhotos}>Save photos</button></div>
        </Modal>
      )}
    </>
  );
}
