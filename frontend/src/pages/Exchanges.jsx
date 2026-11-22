import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errMsg, get, post } from '../api.js';
import { useLoad } from '../hooks.js';
import { useToast } from '../components/Toast.jsx';
import { BookCover, Empty, ErrorNote, Loading, Pill, TrustCard, UserLink } from '../components/ui.jsx';
import { STATUS_TEXT, STATUS_TONE, fmtDate } from '../util.js';

export default function Exchanges() {
  const toast = useToast();
  const [role, setRole] = useState('borrower');
  const list = useLoad(() => get('/requests', { role }), [role]);

  const act = async (id, action, ok) => {
    try { await post(`/requests/${id}/${action}`); toast.ok(ok); list.reload(); } catch (e) { toast.err(errMsg(e)); }
  };

  return (
    <>
      <div className="row between">
        <div><span className="hand">Who has what</span><h1 style={{ fontSize: '2.2rem' }}>My exchanges</h1></div>
        <div className="tabs" role="tablist">
          <button className={role === 'borrower' ? 'on' : ''} onClick={() => setRole('borrower')}>I’m borrowing</button>
          <button className={role === 'owner' ? 'on' : ''} onClick={() => setRole('owner')}>I’m lending</button>
        </div>
      </div>
      <ErrorNote msg={list.error} />
      {list.loading ? <Loading /> : list.data?.length === 0
        ? <Empty icon="🍂" title={role === 'borrower' ? 'You are not borrowing anything yet' : 'Nobody has asked for your books yet'}>
            {role === 'borrower' ? <><Link to="/library">Find a book to borrow</Link>.</> : <><Link to="/shelf">Check your shelf</Link> to make sure books are marked available.</>}
          </Empty>
        : (
          <div className="stack" style={{ marginTop: '1rem' }}>
            {list.data.map((r) => {
              const other = role === 'owner' ? r.borrower : r.owner;
              return (
                <div key={r.id} className="card">
                  <div className="req-card">
                    <Link to={`/exchanges/${r.id}`}><BookCover book={r.book} /></Link>
                    <div className="stack gap-s">
                      <div className="row"><Link to={`/exchanges/${r.id}`}><b style={{ fontFamily: 'var(--serif)', fontSize: '1.15rem' }}>{r.book.title}</b></Link><Pill tone={STATUS_TONE[r.status]}>{STATUS_TEXT[r.status]}</Pill></div>
                      <span className="small muted">{role === 'owner' ? 'Borrower' : 'Owner'}: <UserLink u={other} /> · requested {fmtDate(r.requestedAt)}{r.returnDueAt ? ` · due ${fmtDate(r.returnDueAt)}` : ''}</span>
                    </div>
                    <div className="actions row" style={{ justifyContent: 'flex-end' }}>
                      {role === 'owner' && r.status === 'PENDING' && (
                        <>
                          <button className="btn small" onClick={() => act(r.id, 'accept', 'Request accepted.')}>Accept</button>
                          <button className="btn danger small" onClick={() => act(r.id, 'reject', 'Request declined.')}>Decline</button>
                        </>
                      )}
                      <Link className="btn ghost small" to={`/exchanges/${r.id}`}>Open</Link>
                    </div>
                  </div>
                  {role === 'owner' && r.status === 'PENDING' && (
                    <div style={{ marginTop: '.9rem' }}><TrustCard title={`${r.borrower.name}’s record`} compliance={r.borrowerCompliance} /></div>
                  )}
                </div>
              );
            })}
          </div>
        )}
    </>
  );
}
