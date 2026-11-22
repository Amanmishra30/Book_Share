import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get, post } from '../api.js';
import { useLoad } from '../hooks.js';
import { useToast } from '../components/Toast.jsx';
import { BookBadges, BookCard, BookCover, ErrorNote, Loading, PhotoGrid, TrustCard, UserLink } from '../components/ui.jsx';
import { fmtDate, label, money } from '../util.js';

export default function BookDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const book = useLoad(() => get(`/books/${id}`), [id]);
  const checks = useLoad(() => get(`/books/${id}/condition-checks`), [id]);
  const recs = useLoad(() => get(`/books/${id}/recommendations`), [id]);
  const ownerId = book.data?.owner?.id;
  const owner = useLoad(() => (ownerId ? get(`/users/${ownerId}/profile`) : Promise.resolve(null)), [ownerId]);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  if (book.loading) return <Loading />;
  if (book.error) return <ErrorNote msg={book.error} />;
  const b = book.data;
  const mine = b.owner.id === user.id;
  const canBorrow = b.listingType !== 'SALE';
  const canBuy = b.listingType !== 'BORROW' && b.saleListingId && b.saleStatus === 'LISTED';

  const request = async () => {
    setBusy(true);
    try { const r = await post('/requests', { bookId: b.id, note }); toast.ok('Request sent to the owner.'); nav(`/exchanges/${r.id}`); }
    catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  const interest = async () => {
    setBusy(true);
    try { await post(`/sales/${b.saleListingId}/interest`); nav(`/marketplace/${b.saleListingId}`); }
    catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  const listing = (checks.data || []).filter((c) => c.stage === 'LISTING');

  return (
    <>
      <p><Link to="/library">← Back to the stacks</Link></p>
      <div className="split">
        <div className="stack detail-cover"><BookCover book={b} /><BookBadges book={b} /></div>
        <div className="stack">
          <div>
            <h1>{b.title}</h1>
            <p className="muted" style={{ fontSize: '1.1rem' }}>by {b.author}{b.edition ? ` · ${b.edition}` : ''}{b.genre ? ` · ${b.genre}` : ''}</p>
          </div>
          {b.description && <p>{b.description}</p>}
          <div className="row small muted">
            <span>📍 {b.communityName}</span>
            {b.listingType !== 'SALE' && <span>⏳ lends for {b.lendingDurationDays} days</span>}
            {b.price != null && b.listingType !== 'BORROW' && <span>🏷️ price {money(b.price)} (demo checkout available)</span>}
            <span>shelved {fmtDate(b.createdAt)}</span>
          </div>

          <div className="two">
            <div className="stack gap-s">
              <h3>The keeper of this book</h3>
              <div><UserLink u={b.owner} /></div>
              {owner.data && <TrustCard score={owner.data.reputationScore} ratingCount={owner.data.ratingCount} compliance={owner.data.compliance} />}
            </div>
            <div className="card stitched stack gap-s">
              {mine ? <><h3>This is on your shelf</h3><Link className="btn ghost" to="/shelf">Manage it on My Shelf</Link></> : (
                <>
                  {canBorrow && (
                    <>
                      <h3>Borrow it</h3>
                      <textarea value={note} onChange={(e) => setNote(e.target.value)} placeholder="Say hello and suggest a pickup time (optional)" maxLength={500} />
                      <button className="btn" disabled={busy || !b.available} onClick={request}>{b.available ? 'Request to borrow' : 'Currently on loan'}</button>
                    </>
                  )}
                  {canBuy && (
                    <>
                      <h3>Buy it</h3>
                      <p className="muted small">Demo checkout only: no real money is charged. The listing is marked sold after confirmation.</p>
                      <button className="btn honey" disabled={busy} onClick={interest}>I’m interested</button>
                    </>
                  )}
                  {!canBorrow && !canBuy && <p className="muted">This book is not available right now.</p>}
                </>
              )}
            </div>
          </div>

          <div>
            <h3>How it looked when it was shelved</h3>
            {listing.length === 0 ? <p className="muted small">The owner has not added condition photos yet.</p>
              : listing.map((c) => <div key={c.id} className="stack gap-s"><PhotoGrid urls={c.photoUrls} /><span className="muted small">Photographed {fmtDate(c.createdAt)} · condition on record: {label(b.condition)}</span></div>)}
          </div>
        </div>
      </div>

      {recs.data?.length > 0 && (
        <>
          <div className="section-title"><h2>Readers who borrowed this also borrowed</h2></div>
          <div className="grid">{recs.data.map((r) => <BookCard key={r.id} book={r} />)}</div>
        </>
      )}
    </>
  );
}
