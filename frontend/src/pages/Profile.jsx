import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get, put } from '../api.js';
import { useLoad } from '../hooks.js';
import { useToast } from '../components/Toast.jsx';
import { ErrorNote, Loading, Pill, Stars, TrustCard } from '../components/ui.jsx';
import { fmtDate, label } from '../util.js';
import profileBackground from '../assets/profile-background.png';

const STATUS_HELP = {
  WARNED: 'You have 1 valid report. This is only a warning; nothing is restricted.',
  RESTRICTED: 'You have 2 valid reports. You cannot borrow for a while, but you can still lend.',
  UNDER_REVIEW: 'You have 3 or more valid reports. An admin is reviewing your account.',
  SUSPENDED: 'Your account is suspended.'
};

export default function Profile() {
  const { id } = useParams();
  const { user, refresh } = useAuth();
  const toast = useToast();
  const p = useLoad(() => get(`/users/${id}/profile`), [id]);
  const [edit, setEdit] = useState(null);

  if (p.loading) return <Loading />;
  if (p.error) return <ErrorNote msg={p.error} />;
  const d = p.data;
  const mine = d.user.id === user.id;

  const save = async (e) => {
    e.preventDefault();
    try { await put('/users/me', edit); toast.ok('Profile saved.'); setEdit(null); await refresh(); p.reload(); } catch (er) { toast.err(errMsg(er)); }
  };
  const bar = (name, v) => <div key={name}><div className="row between small"><span>{name}</span><b>{v.toFixed(1)}</b></div><div className="bar"><i style={{ width: `${(v / 5) * 100}%` }} /></div></div>;

  return (
    <>
      <div
  className={`card pad-l stitched profile-hero ${mine ? 'profile-hero-own' : ''}`}
  style={
    mine
      ? { '--profile-bg': `url("${profileBackground}")` }
      : undefined
  }
>
        <div className="row" style={{ gap: '1.4rem' }}>
          <div className="avatar big">{d.user.name[0].toUpperCase()}</div>
          <div className="grow">
            <h1 style={{ marginBottom: '.2rem' }}>{d.user.name}</h1>
            <span className="muted">📍 {d.user.communityName} · reader since {fmtDate(d.memberSince)}</span>
            {d.bio && <p style={{ marginTop: '.6rem' }}>{d.bio}</p>}
          </div>
          {mine && <button className="btn ghost small" onClick={() => setEdit({ name: d.user.name, bio: d.bio || '' })}>Edit profile</button>}
        </div>
        {d.accountStatus && d.accountStatus !== 'ACTIVE' && (
          <div className="note" style={{ marginTop: '1rem' }}><Pill tone="rose">{label(d.accountStatus)}</Pill> {STATUS_HELP[d.accountStatus]}</div>
        )}
      </div>

      <div className="two" style={{ marginTop: '1.2rem' }}>
        <div className="card stack gap-s">
          <h3>Star rating</h3>
          {d.ratingCount === 0 ? <p className="muted">No ratings yet.</p> : (
            <>
              <div className="row"><span style={{ fontSize: '2.2rem', fontFamily: 'var(--serif)' }}>{d.reputationScore.toFixed(1)}</span><Stars value={d.reputationScore} /><span className="muted small">from {d.ratingCount} rating{d.ratingCount > 1 ? 's' : ''}</span></div>
              {bar('Communication', d.avgCommunication)}{bar('Care for books', d.avgCare)}{bar('Reliability', d.avgReliability)}
            </>
          )}
        </div>
        <div className="stack gap-s">
          <h3>Return record</h3>
          <TrustCard compliance={d.compliance} />
          <span className="small muted">Shown separately from stars so a friendly average never hides a poor return record. Counts come straight from the exchange history.</span>
        </div>
      </div>

      <div className="section-title"><h2>What readers said</h2></div>
      {d.recentRatings.length === 0 ? <p className="muted">Nothing yet.</p> : (
        <div className="stack">{d.recentRatings.map((r) => (
          <div key={r.id} className="card"><div className="row"><b>{r.rater.name}</b><Stars value={(r.communication + r.care + r.reliability) / 3} /><span className="small muted">{fmtDate(r.createdAt)}</span></div>{r.comment && <p style={{ margin: '.3rem 0 0' }}>“{r.comment}”</p>}</div>
        ))}</div>
      )}

      {edit && (
        <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && setEdit(null)}>
          <form className="modal stack" onSubmit={save}>
            <h3>Edit profile</h3>
            <label className="field">Name<input value={edit.name} maxLength={80} onChange={(e) => setEdit({ ...edit, name: e.target.value })} /></label>
            <label className="field">About you<textarea value={edit.bio} maxLength={500} onChange={(e) => setEdit({ ...edit, bio: e.target.value })} /></label>
            <div className="row"><button className="btn">Save</button><button type="button" className="btn ghost" onClick={() => setEdit(null)}>Cancel</button></div>
          </form>
        </div>
      )}
    </>
  );
}
