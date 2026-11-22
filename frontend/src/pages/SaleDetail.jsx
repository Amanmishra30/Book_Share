import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get, post, put } from '../api.js';
import { useLoad } from '../hooks.js';
import ChatBox from '../components/ChatBox.jsx';
import { useToast } from '../components/Toast.jsx';
import { BookCover, ErrorNote, Loading, PhotoGrid, Pill } from '../components/ui.jsx';
import { STATUS_TONE, fmtDate, label, money } from '../util.js';

export default function SaleDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const sale = useLoad(() => get(`/sales/${id}`), [id]);
  const threads = useLoad(() => get(`/sales/${id}/threads`), [id]);
  const checks = useLoad(() => get(`/books/${sale.data?.book?.id}/condition-checks`).catch(() => []), [sale.data?.book?.id]);
  const [picked, setPicked] = useState(null);
  const [price, setPrice] = useState('');
  const [busy, setBusy] = useState(false);
  const [demoPaid, setDemoPaid] = useState(false);

  if (sale.loading && !sale.data) return <Loading />;
  if (sale.error) return <ErrorNote msg={sale.error} />;
  const s = sale.data;
  const isSeller = s.seller.id === user.id;
  const myThread = !isSeller && (threads.data || []).length > 0;
  const buyerId = isSeller ? picked : (myThread ? user.id : null);
  const open = s.status === 'LISTED' || s.status === 'RESERVED';

  const run = async (fn, ok) => {
    setBusy(true);
    try { await fn(); if (ok) toast.ok(ok); await sale.reload(); await threads.reload(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  const photos = (checks.data || []).filter((c) => c.stage === 'LISTING');

  return (
    <>
      <p><Link to="/marketplace">← Marketplace</Link></p>
      <div className="split">
        <div className="stack detail-cover"><BookCover book={s.book} /></div>
        <div className="stack">
          <div className="row"><h1 style={{ marginBottom: 0 }}><Link to={`/books/${s.book.id}`}>{s.book.title}</Link></h1><Pill tone={STATUS_TONE[s.status]}>{label(s.status)}</Pill></div>
          <p className="muted">by {s.book.author} · {label(s.book.condition)} · listed by <Link to={`/profile/${s.seller.id}`}>{s.seller.name}</Link> on {fmtDate(s.createdAt)}</p>
          <p style={{ fontSize: '1.3rem' }}><b>Asking {money(s.book.price)}</b>{s.agreedPrice != null && <> · agreed price <b>{money(s.agreedPrice)}</b></>}</p>
          {s.buyer && <p className="note">Reserved for {s.buyer.name}.</p>}
          <div className="note"><b>Demo payment mode:</b> this checkout only demonstrates the payment flow. No real money is collected and no Razorpay transaction is created.</div>
          {photos.length > 0 && <div><h3>Condition when listed</h3>{photos.map((c) => <PhotoGrid key={c.id} urls={c.photoUrls} />)}</div>}

          {isSeller && open && (
            <div className="card stitched stack gap-s">
              <h3>Seller’s desk</h3>
              <p className="small muted">Pick a conversation on the right, then reserve the book for that buyer. After you meet in person, mark it sold.</p>
              <div className="row">
                {s.status === 'LISTED' && (
                  <>
                    <input style={{ width: 150 }} type="number" min="0" step="0.01" placeholder={`Price (${money(s.book.price)})`} value={price} onChange={(e) => setPrice(e.target.value)} />
                    <button className="btn" disabled={busy || !picked} onClick={() => run(() => put(`/sales/${id}/reserve`, { buyerId: picked, agreedPrice: price === '' ? null : Number(price) }), 'Reserved.')}>Reserve for this buyer</button>
                  </>
                )}
                {s.status === 'RESERVED' && (
                  <>
                    <button className="btn" disabled={busy} onClick={() => run(() => put(`/sales/${id}/sold`), 'Marked as sold.')}>Mark sold</button>
                    <button className="btn ghost" disabled={busy} onClick={() => run(() => put(`/sales/${id}/release`), 'Reservation released.')}>Release</button>
                  </>
                )}
                <button className="btn danger" disabled={busy} onClick={() => window.confirm('Cancel this listing?') && run(() => put(`/sales/${id}/cancel`), 'Listing cancelled.')}>Cancel listing</button>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="section-title"><h2>{isSeller ? 'Interested buyers' : 'Chat with the seller'}</h2></div>
      {isSeller ? (
        <div className="split" style={{ gridTemplateColumns: '280px 1fr' }}>
          <div className="stack gap-s">
            {(threads.data || []).length === 0 && <p className="muted">Nobody has messaged yet.</p>}
            {(threads.data || []).map((t) => (
              <button key={t.buyer.id} className={`card ${picked === t.buyer.id ? 'stitched' : ''}`} style={{ textAlign: 'left', cursor: 'pointer' }} onClick={() => setPicked(t.buyer.id)}>
                <b>{t.buyer.name}</b><div className="small muted">{t.lastMessage}</div>
              </button>
            ))}
          </div>
          {buyerId ? <ChatBox key={buyerId} meId={user.id} topic={`/topic/sale.${id}.${buyerId}`} disabled={!open}
              loadHistory={() => get(`/chat/sale/${id}/messages`, { buyerId })} send={(content) => post(`/chat/sale/${id}/messages`, { content, threadUserId: buyerId })} />
            : <div className="empty">Choose a buyer to read the conversation.</div>}
        </div>
      ) : (
        <div className="stack">
          {s.status === 'SOLD' ? (
            <div className="card center stack"><Pill tone="success">SOLD</Pill><h3>Book sold successfully</h3><p className="muted small">Demo payment reference: {s.demoPaymentReference || 'Recorded in the backend'}. No real payment was made.</p></div>
          ) : (
            <div className="card center stack">
              <p>{open ? 'You can start a conversation with the seller or try the demo checkout.' : 'This listing is closed.'}</p>
              {open && <div className="row" style={{ justifyContent: 'center', flexWrap: 'wrap' }}>
                {!myThread && <button className="btn ghost" disabled={busy} onClick={() => run(() => post(`/sales/${id}/interest`), 'Chat opened.')}>I’m interested</button>}
                <button className="btn honey" disabled={busy} onClick={async () => {
                  const ok = window.confirm(`Simulate payment of ${money(s.book.price)}? This is a demo only. No real money will be charged.`);
                  if (!ok) return;
                  await run(() => post(`/sales/${id}/simulate-payment`, {}), 'Demo payment successful. The book is now marked sold.');
                  setDemoPaid(true);
                }}> {busy ? 'Processing…' : `Simulate payment · ${money(s.book.price)}`} </button>
              </div>}
              {demoPaid && <p className="small" role="status">Demo checkout completed. No real payment was made.</p>}
            </div>
          )}
          {myThread && open && <ChatBox meId={user.id} topic={`/topic/sale.${id}.${user.id}`} disabled={!open}
            loadHistory={() => get(`/chat/sale/${id}/messages`, { buyerId: user.id })} send={(content) => post(`/chat/sale/${id}/messages`, { content, threadUserId: user.id })} />}
        </div>
      )}
    </>
  );
}
