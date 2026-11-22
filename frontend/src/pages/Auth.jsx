import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get } from '../api.js';
import { useLoad } from '../hooks.js';
import { ErrorNote } from '../components/ui.jsx';

export function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try { await login(f.email, f.password); nav(loc.state?.from || '/library'); } catch (er) { setError(errMsg(er)); } finally { setBusy(false); }
  };
  return (
    <div className="card pad-l stitched" style={{ maxWidth: 460, margin: '2rem auto' }}>
      <span className="hand">Welcome back</span>
      <h2>Show your library card</h2>
      <form onSubmit={submit} className="stack">
        <label className="field">Email<input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} autoComplete="email" /></label>
        <label className="field">Password<input type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete="current-password" /></label>
        <ErrorNote msg={error} />
        <button className="btn" disabled={busy}>{busy ? 'Opening the door…' : 'Step inside'}</button>
      </form>
      <p className="muted small">New here? <Link to="/register">Join the library</Link></p>
    </div>
  );
}

export function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const communities = useLoad(() => get('/communities/public'), []);
  const [f, setF] = useState({ name: '', email: '', password: '', communityId: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try { await register({ ...f, communityId: Number(f.communityId) }); nav('/library'); } catch (er) { setError(errMsg(er)); } finally { setBusy(false); }
  };
  return (
    <div className="card pad-l stitched" style={{ maxWidth: 520, margin: '2rem auto' }}>
      <span className="hand">Every library needs readers</span>
      <h2>Join your community</h2>
      <form onSubmit={submit} className="stack">
        <label className="field">Your name<input required value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} maxLength={80} /></label>
        <label className="field">Email<input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} autoComplete="email" /></label>
        <label className="field">Password (8+ characters)<input type="password" required minLength={8} value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} autoComplete="new-password" /></label>
        <label className="field">Where do you live?
          <select required value={f.communityId} onChange={(e) => setF({ ...f, communityId: e.target.value })}>
            <option value="">Choose your building, hostel or neighbourhood…</option>
            {(communities.data || []).map((c) => <option key={c.id} value={c.id}>{c.path} ({c.level.toLowerCase()})</option>)}
          </select></label>
        <ErrorNote msg={error || communities.error} />
        <button className="btn" disabled={busy}>{busy ? 'Making your card…' : 'Get my library card'}</button>
      </form>
      <p className="muted small">Already a member? <Link to="/login">Sign in</Link></p>
    </div>
  );
}
