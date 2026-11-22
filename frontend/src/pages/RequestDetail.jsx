import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get, multipart, post } from '../api.js';
import { useLoad } from '../hooks.js';
import ChatBox from '../components/ChatBox.jsx';
import { useToast } from '../components/Toast.jsx';
import { BookCover, ErrorNote, Loading, Modal, PhotoGrid, PhotoPicker, Pill, Stars, StarInput, TrustCard, UserLink, emptySlots, pickedFiles } from '../components/ui.jsx';
import { STATUS_TEXT, STATUS_TONE, fmtDate, fmtDateTime, label } from '../util.js';

const STEPS = ['Requested', 'Accepted', 'Handed over', 'Borrowed', 'Returned', 'Completed'];
const IDX = { PENDING: 0, ACCEPTED: 1, PICKED_UP: 2, BORROWED: 3, RETURN_DUE: 3, RETURNED: 4, COMPLETED: 5, REJECTED: 0 };

function Trail({ status }) {
  const now = IDX[status];
  return (
    <div className="trail" aria-label="Progress of this exchange">
      {STEPS.map((s, i) => {
        const cls = status === 'COMPLETED' || i < now ? 'done' : i === now ? `now ${status === 'RETURN_DUE' || status === 'REJECTED' ? 'bad' : ''}` : '';
        return <div key={s} className={`step ${cls}`}>{s}</div>;
      })}
    </div>
  );
}

function CheckColumns({ checks }) {
  const col = (stage, title) => {
    const list = checks.filter((c) => c.stage === stage);
    return (
      <div className="col" key={stage}>
        <h4>{title}</h4>
        {list.length === 0 ? <p className="muted small">No photos yet.</p> : list.map((c) => (
          <div key={c.id} className="stack gap-s" style={{ marginBottom: '.8rem' }}>
            <PhotoGrid urls={c.photoUrls} />
            <span className="muted small">By {c.loggedBy?.name} · {fmtDateTime(c.createdAt)}</span>
            {c.aiNotes && <span className="note small">Automatic note: {c.aiNotes}</span>}
          </div>
        ))}
      </div>
    );
  };
  return <div className="compare">{col('LISTING', 'When shelved')}{col('PICKUP', 'At pickup')}{col('RETURN', 'At return')}</div>;
}
export { CheckColumns };

export default function RequestDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const toast = useToast();
  const d = useLoad(() => get(`/requests/${id}`), [id]);
  const [busy, setBusy] = useState(false);
  const [photoStage, setPhotoStage] = useState(null);
  const [slots, setSlots] = useState(emptySlots());
  const [report, setReport] = useState(null);
  const [rate, setRate] = useState({ communication: 5, care: 5, reliability: 5, comment: '' });
  const [error, setError] = useState('');

  if (d.loading && !d.data) return <Loading />;
  if (d.error) return <ErrorNote msg={d.error} />;
  const { request: r, checks, reports, ratings } = d.data;
  const isOwner = r.owner.id === user.id;
  const other = isOwner ? r.borrower : r.owner;
  const hasPickup = checks.some((c) => c.stage === 'PICKUP');
  const hasReturn = checks.some((c) => c.stage === 'RETURN');

  const act = async (action, ok) => {
    setBusy(true);
    try { await post(`/requests/${id}/${action}`); toast.ok(ok); await d.reload(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  const upload = async () => {
    const files = pickedFiles(slots);
    if (files.length < 3) return setError('Please add 3 to 5 photos: front cover, back cover, spine and fanned pages.');
    setBusy(true); setError('');
    try {
      await multipart('post', `/requests/${id}/condition-checks`, { fields: { stage: photoStage }, files: { photos: files } });
      toast.ok('Photos saved to the book’s history.');
      setPhotoStage(null); setSlots(emptySlots()); await d.reload();
    } catch (e) { setError(errMsg(e)); } finally { setBusy(false); }
  };
  const sendReport = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      await multipart('post', `/requests/${id}/reports`, { fields: { category: report.category, details: report.details }, files: { photos: report.files } });
      toast.ok('Report sent. An admin will review it with the photos.');
      setReport(null); await d.reload();
    } catch (er) { setError(errMsg(er)); } finally { setBusy(false); }
  };
  const sendRating = async (e) => {
    e.preventDefault();
    setBusy(true);
    try { await post(`/requests/${id}/ratings`, rate); toast.ok('Thanks for rating!'); await d.reload(); } catch (er) { toast.err(errMsg(er)); } finally { setBusy(false); }
  };

  const afterReturn = ['RETURNED', 'COMPLETED'].includes(r.status);
  const canNotReturned = isOwner && r.status === 'RETURN_DUE' && r.reminderSentAt && new Date() >= new Date(r.notReturnedReportableAt);
  const reportCats = [...(afterReturn ? ['DAMAGED', 'MISSING_PAGES', 'OTHER'] : []), ...(canNotReturned ? ['NOT_RETURNED'] : [])];

  return (
    <>
      <p><Link to="/exchanges">← My exchanges</Link></p>
      <div className="card">
        <div className="req-card">
          <BookCover book={r.book} />
          <div className="stack gap-s">
            <h2 style={{ marginBottom: 0 }}><Link to={`/books/${r.book.id}`}>{r.book.title}</Link></h2>
            <span className="muted">{r.book.author}</span>
            <div className="row"><Pill tone={STATUS_TONE[r.status]}>{STATUS_TEXT[r.status]}</Pill>
              <span className="small muted">Borrower <UserLink u={r.borrower} /> · Owner <UserLink u={r.owner} /></span></div>
            {r.note && <p className="small">“{r.note}”</p>}
          </div>
        </div>
        {r.status === 'REJECTED' ? <p className="note" style={{ marginTop: '1rem' }}>The owner declined this request.</p> : <Trail status={r.status} />}
        <div className="row small muted">
          <span>Requested {fmtDate(r.requestedAt)}</span>{r.acceptedAt && <span>· accepted {fmtDate(r.acceptedAt)}</span>}
          {r.pickedUpAt && <span>· handed over {fmtDate(r.pickedUpAt)}</span>}{r.returnDueAt && <span>· due back <b>{fmtDate(r.returnDueAt)}</b></span>}
          {r.returnedAt && <span>· returned {fmtDate(r.returnedAt)}</span>}
        </div>
      </div>

      <div className="two" style={{ marginTop: '1.2rem' }}>
        <div className="card stitched stack">
          <h3>What happens next</h3>
          {r.status === 'PENDING' && (isOwner ? (
            <><p>Check {r.borrower.name}’s record, then decide.</p>
              <div className="row"><button className="btn" disabled={busy} onClick={() => act('accept', 'Request accepted.')}>Accept</button>
                <button className="btn danger" disabled={busy} onClick={() => act('reject', 'Request declined.')}>Decline</button></div></>
          ) : <p>Waiting for {r.owner.name} to reply. You can chat below in the meantime.</p>)}

          {r.status === 'ACCEPTED' && (
            <>
              <p>Meet up and take a few photos of the book together. The owner can hand it over once pickup photos exist.</p>
              <div className="row">
                <button className="btn honey" onClick={() => { setError(''); setPhotoStage('PICKUP'); }}>{hasPickup ? 'Add more pickup photos' : 'Add pickup photos'}</button>
                {isOwner && <button className="btn" disabled={busy || !hasPickup} onClick={() => act('pickup', 'Marked as handed over.')}>Mark handed over</button>}
              </div>
              {isOwner && !hasPickup && <span className="small muted">The hand-over button unlocks after pickup photos are added.</span>}
            </>
          )}
          {r.status === 'PICKED_UP' && (isOwner ? <p>Waiting for {r.borrower.name} to confirm they received the book.</p>
            : <><p>Did you get the book? Confirm to start the loan.</p><button className="btn" disabled={busy} onClick={() => act('receive', 'Enjoy the read!')}>I received the book</button></>)}

          {['BORROWED', 'RETURN_DUE'].includes(r.status) && (
            <>
              {r.status === 'RETURN_DUE' && <div className="note err">This book is overdue. A reminder was posted in the chat{r.reminderSentAt ? ` on ${fmtDate(r.reminderSentAt)}` : ''}.</div>}
              {isOwner ? <p>The book is with {r.borrower.name}. You will be asked to confirm once it is back.</p>
                : (
                  <>
                    <p>When you return the book, photograph it again. Then mark it returned.</p>
                    <div className="row">
                      <button className="btn honey" onClick={() => { setError(''); setPhotoStage('RETURN'); }}>{hasReturn ? 'Add more return photos' : 'Add return photos'}</button>
                      <button className="btn" disabled={busy || !hasReturn} onClick={() => act('return', 'Marked as returned.')}>Mark returned</button>
                    </div>
                    {!hasReturn && <span className="small muted">Marking returned unlocks after return photos are added.</span>}
                  </>
                )}
              {isOwner && <button className="btn ghost small" onClick={() => { setError(''); setPhotoStage('RETURN'); }}>Add return photos (owner view)</button>}
            </>
          )}
          {r.status === 'RETURNED' && (isOwner
            ? <><p>Compare the photos below. If the book came back fine, confirm to complete the exchange.</p><button className="btn" disabled={busy} onClick={() => act('complete', 'Exchange completed.')}>Confirm: all good</button></>
            : <p>Returned. Waiting for {r.owner.name} to confirm.</p>)}
          {r.status === 'COMPLETED' && <p>This exchange is complete. 🌿</p>}

          {r.status === 'RETURN_DUE' && isOwner && !canNotReturned && (
            <p className="small muted">You can report the book as not returned after {fmtDate(r.notReturnedReportableAt)} (grace period) once a reminder has been sent.</p>
          )}
          {reportCats.length > 0 && <button className="btn ghost small" onClick={() => { setError(''); setReport({ category: reportCats[0], details: '', files: [] }); }}>Report a problem</button>}
        </div>

        <div className="stack">
          <TrustCard title={`${r.borrower.name} · as a borrower`} compliance={r.borrowerCompliance} />
          <TrustCard title={`${r.owner.name} · as a lender`} compliance={r.ownerCompliance} />
        </div>
      </div>

      {r.status === 'COMPLETED' && (
        <div className="card" style={{ marginTop: '1.2rem' }}>
          <h3>Ratings</h3>
          {ratings.map((x) => (
            <p key={x.id} className="small"><b>{x.rater.name}</b> rated {x.ratee.name}: <Stars value={(x.communication + x.care + x.reliability) / 3} /> <span className="muted">(communication {x.communication}, care {x.care}, reliability {x.reliability})</span>{x.comment ? ` — “${x.comment}”` : ''}</p>
          ))}
          {!r.iRated && (
            <form onSubmit={sendRating} className="stack gap-s">
              <b>Rate {other.name}</b>
              {[['communication', 'Communication'], ['care', 'Care for the book / reliability of the book’s description'], ['reliability', 'Reliability']].map(([k, t]) => (
                <div key={k} className="row between"><span>{k === 'care' ? 'Care' : t}</span><StarInput value={rate[k]} onChange={(v) => setRate({ ...rate, [k]: v })} /></div>
              ))}
              <textarea value={rate.comment} onChange={(e) => setRate({ ...rate, comment: e.target.value })} placeholder="A kind word (optional)" maxLength={1000} />
              <div><button className="btn" disabled={busy}>Send rating</button></div>
            </form>
          )}
          {r.iRated && !r.theyRated && <p className="muted small">You rated this exchange. Waiting for {other.name} to rate you.</p>}
        </div>
      )}

      <div className="section-title"><h2>Condition photos</h2><span className="hand">side by side, so everyone sees the same thing</span></div>
      <CheckColumns checks={checks} />

      {reports.length > 0 && (
        <>
          <div className="section-title"><h2>Reports</h2></div>
          <div className="stack">
            {reports.map((x) => (
              <div key={x.id} className="card">
                <div className="row"><Pill tone="rose">{label(x.category)}</Pill><Pill tone={x.status === 'VALID' ? 'rose' : x.status === 'INVALID' ? 'muted' : 'honey'}>{label(x.status)}</Pill>
                  <span className="small muted">filed by {x.reporter.name} on {fmtDate(x.createdAt)}</span></div>
                {x.details && <p>{x.details}</p>}
                {x.evidencePhotoUrls.length > 0 && <PhotoGrid urls={x.evidencePhotoUrls} />}
                {x.adminNote && <p className="note small">Admin note: {x.adminNote}</p>}
              </div>
            ))}
          </div>
        </>
      )}

      <div className="section-title"><h2>Chat with {other.name}</h2></div>
      <ChatBox meId={user.id} topic={`/topic/request.${id}`} loadHistory={() => get(`/chat/request/${id}/messages`)}
        send={(content) => post(`/chat/request/${id}/messages`, { content })} />

      {photoStage && (
        <Modal title={photoStage === 'PICKUP' ? 'Pickup photos' : 'Return photos'} onClose={() => { setPhotoStage(null); setSlots(emptySlots()); }}>
          <p className="muted small">Take 3 to 5 photos: front cover, back cover, spine, and a fanned-page shot (flick the pages so the edges show).</p>
          <PhotoPicker files={slots} onChange={setSlots} />
          <ErrorNote msg={error} />
          <div style={{ marginTop: '1rem' }}><button className="btn" disabled={busy} onClick={upload}>{busy ? 'Uploading…' : 'Save photos'}</button></div>
        </Modal>
      )}
      {report && (
        <Modal title="Report a problem" onClose={() => setReport(null)}>
          <form onSubmit={sendReport} className="stack">
            <p className="muted small">Reports never punish anyone automatically. An admin reviews them together with the photos above.</p>
            <label className="field">What happened?
              <select value={report.category} onChange={(e) => setReport({ ...report, category: e.target.value })}>{reportCats.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select></label>
            <label className="field">Details<textarea value={report.details} onChange={(e) => setReport({ ...report, details: e.target.value })} maxLength={2000} /></label>
            <label className="field">Extra photos (optional, up to 5)<input type="file" accept="image/*" multiple onChange={(e) => setReport({ ...report, files: [...e.target.files].slice(0, 5) })} /></label>
            <ErrorNote msg={error} />
            <div><button className="btn danger" disabled={busy}>Send report</button></div>
          </form>
        </Modal>
      )}
    </>
  );
}
