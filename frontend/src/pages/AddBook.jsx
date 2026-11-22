import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errMsg, multipart } from '../api.js';
import BookForm from '../components/BookForm.jsx';
import { useToast } from '../components/Toast.jsx';
import { ErrorNote } from '../components/ui.jsx';

export default function AddBook() {
  const nav = useNavigate();
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async ({ book, cover, photos }) => {
    setBusy(true); setError('');
    try {
      const created = await multipart('post', '/books', { json: { book }, files: { cover, photos } });
      toast.ok('Shelved! Your book is now in the community library.');
      nav(`/books/${created.id}`);
    } catch (e) { setError(errMsg(e)); } finally { setBusy(false); }
  };
  return (
    <>
      <p><Link to="/shelf">← My shelf</Link></p>
      <div className="card pad-l stitched">
        <span className="hand">Another story for the woods</span>
        <h1 style={{ fontSize: '2rem' }}>Shelve a book</h1>
        <BookForm onSubmit={submit} busy={busy} />
        <ErrorNote msg={error} />
      </div>
    </>
  );
}
