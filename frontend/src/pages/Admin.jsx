import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errMsg, get, post, put } from '../api.js';
import { useLoad } from '../hooks.js';
import { useToast } from '../components/Toast.jsx';
import { ErrorNote, Loading, PhotoGrid, Pill } from '../components/ui.jsx';
import { CheckColumns } from './RequestDetail.jsx';
import { fmtDate, label, money } from '../util.js';

function ReportCard({ r, onSaved }) {
  const toast = useToast();
  const [note, setNote] = useState(r.adminNote || '');
  const [busy, setBusy] = useState(false);
  const decide = async (status) => {
    setBusy(true);
    try { await put(`/admin/reports/${r.id}`, { status, adminNote: note }); toast.ok(`Report marked ${label(status).toLowerCase()}.`); onSaved(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  return (
    <div className="card stack">
      <div className="row">
        <Pill tone="rose">{label(r.category)}</Pill>
        <Pill tone={r.status === 'VALID' ? 'rose' : r.status === 'INVALID' ? 'muted' : 'honey'}>{label(r.status)}</Pill>
        <b>{r.bookTitle}</b><span className="small muted">{fmtDate(r.createdAt)} · <Link to={`/profile/${r.reporter.id}`}>{r.reporter.name}</Link> reported <Link to={`/profile/${r.reportedUser.id}`}>{r.reportedUser.name}</Link></span>
      </div>
      {r.details && <p>“{r.details}”</p>}
      {r.evidencePhotoUrls.length > 0 && <div><b className="small">Extra photos from the reporter</b><PhotoGrid urls={r.evidencePhotoUrls} /></div>}
      <CheckColumns checks={r.checks || []} />
      <label className="field">Admin note<textarea value={note} onChange={(e) => setNote(e.target.value)} maxLength={1000} /></label>
      <div className="row">
        <button className="btn ghost small" disabled={busy} onClick={() => decide('UNDER_REVIEW')}>Mark under review</button>
        <button className="btn danger small" disabled={busy} onClick={() => decide('VALID')}>Valid: counts against the user</button>
        <button className="btn small" disabled={busy} onClick={() => decide('INVALID')}>Invalid: dismiss</button>
      </div>
    </div>
  );
}

export default function Admin() {
  const toast = useToast();
  const [tab, setTab] = useState('overview');
  const [filter, setFilter] = useState('OPEN');
  const stats = useLoad(() => get('/admin/stats'), []);
  const reports = useLoad(() => get('/admin/reports', { status: filter }), [filter]);
  const flagged = useLoad(() => get('/admin/flagged-users'), []);
  const sales = useLoad(() => get('/admin/sales'), []);
  const communities = useLoad(() => get('/communities'), []);
  const [c, setC] = useState({ name: '', level: 'BUILDING', parentId: '' });

  const setStatus = async (uid, status) => {
    try { await put(`/admin/users/${uid}/status`, { status }); toast.ok('Account status updated.'); flagged.reload(); } catch (e) { toast.err(errMsg(e)); }
  };
  const addCommunity = async (e) => {
    e.preventDefault();
    try { await post('/communities', { name: c.name, level: c.level, parentId: c.parentId ? Number(c.parentId) : null }); toast.ok('Community created.'); setC({ name: '', level: 'BUILDING', parentId: '' }); communities.reload(); } catch (er) { toast.err(errMsg(er)); }
  };
  const refreshAll = () => { reports.reload(); flagged.reload(); stats.reload(); };

  return (
    <>
      <span className="hand">Keeper of the keys</span>
      <h1 style={{ fontSize: '2.2rem' }}>Admin dashboard</h1>
      <div className="tabs" style={{ margin: '.5rem 0 1.2rem' }}>
        {[['overview', 'Overview'], ['reports', 'Reports'], ['users', 'Flagged users'], ['sales', 'Active sales']].map(([k, t]) => <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{t}</button>)}
      </div>

      {tab === 'overview' && (
        <>
          {stats.loading ? <Loading /> : stats.data && (
            <div className="stats">
              {[['Readers', stats.data.users], ['Books on shelves', stats.data.books], ['Open exchanges', stats.data.openExchanges], ['Reports to review', stats.data.pendingReports], ['Active sales', stats.data.activeSales]].map(([k, v]) => <div key={k} className="card stat"><b>{v}</b><span className="muted small">{k}</span></div>)}
            </div>
          )}
          <div className="card stack" style={{ marginTop: '1.2rem' }}>
            <h3>Add a community</h3>
            <form onSubmit={addCommunity} className="form-grid">
              <label className="field">Name<input required value={c.name} onChange={(e) => setC({ ...c, name: e.target.value })} /></label>
              <label className="field">Level<select value={c.level} onChange={(e) => setC({ ...c, level: e.target.value })}><option value="BUILDING">Building</option><option value="NEIGHBOURHOOD">Neighbourhood</option><option value="CITY">City</option></select></label>
              <label className="field full">Parent<select value={c.parentId} onChange={(e) => setC({ ...c, parentId: e.target.value })}><option value="">None (top level)</option>{(communities.data || []).map((x) => <option key={x.id} value={x.id}>{x.path}</option>)}</select></label>
              <div><button className="btn">Create</button></div>
            </form>
            <div className="small muted">{(communities.data || []).map((x) => <div key={x.id}>{x.path} <em>({x.level.toLowerCase()})</em></div>)}</div>
          </div>
        </>
      )}

      {tab === 'reports' && (
        <>
          <label className="field" style={{ maxWidth: 240, marginBottom: '1rem' }}>Show<select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="OPEN">Waiting for review</option><option value="VALID">Valid</option><option value="INVALID">Invalid</option><option value="ALL">Everything</option></select></label>
          <ErrorNote msg={reports.error} />
          {reports.loading ? <Loading /> : reports.data.length === 0 ? <p className="muted">Nothing here. 🌿</p> : <div className="stack">{reports.data.map((r) => <ReportCard key={`${r.id}-${r.status}`} r={r} onSaved={refreshAll} />)}</div>}
        </>
      )}

      {tab === 'users' && (
        <div className="card">
          <p className="small muted">Escalation: 1 valid report → warning, 2 → cannot borrow for a while (can still lend), 3 or more → account review. Only reports you mark valid count.</p>
          {flagged.loading ? <Loading /> : flagged.data.length === 0 ? <p className="muted">No flagged accounts.</p> : (
            <div style={{ overflowX: 'auto' }}><table className="tbl">
              <thead><tr><th>Reader</th><th>Status</th><th>Valid / total reports</th><th>Restricted until</th><th>Set status</th></tr></thead>
              <tbody>{flagged.data.map((f) => (
                <tr key={f.user.id}>
                  <td><Link to={`/profile/${f.user.id}`}>{f.user.name}</Link></td><td><Pill tone={f.accountStatus === 'ACTIVE' ? 'moss' : 'rose'}>{label(f.accountStatus)}</Pill></td>
                  <td>{f.validReports} / {f.totalReports}</td><td>{f.restrictedUntil ? fmtDate(f.restrictedUntil) : '—'}</td>
                  <td><select value={f.accountStatus} onChange={(e) => setStatus(f.user.id, e.target.value)}>{['ACTIVE', 'WARNED', 'RESTRICTED', 'UNDER_REVIEW', 'SUSPENDED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}</select></td>
                </tr>))}</tbody>
            </table></div>
          )}
        </div>
      )}

      {tab === 'sales' && (
        <div className="card">
          {sales.loading ? <Loading /> : sales.data.length === 0 ? <p className="muted">No active sale listings.</p> : (
            <div style={{ overflowX: 'auto' }}><table className="tbl">
              <thead><tr><th>Book</th><th>Seller</th><th>Price</th><th>Status</th><th>Listed</th></tr></thead>
              <tbody>{sales.data.map((s) => <tr key={s.id}><td><Link to={`/marketplace/${s.id}`}>{s.book.title}</Link></td><td>{s.seller.name}</td><td>{money(s.book.price)}</td><td><Pill tone="honey">{label(s.status)}</Pill></td><td>{fmtDate(s.createdAt)}</td></tr>)}</tbody>
            </table></div>
          )}
        </div>
      )}
    </>
  );
}
