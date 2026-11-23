import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './authContext.jsx';
import loginBackground from './assets/login background.png';
import { errMsg, get } from './api.js';
import { useLoad } from './hooks.js';
import { ErrorNote } from './components/ui.jsx';
import './khwabnama-auth-v6.css';

function AuthFrame({ eyebrow, title, intro, children, footer, storyTitle, storyCopy }) {
  return  <section
    className="kw-auth-page"
    style={{ '--login-bg': `url(${loginBackground})` }}
  >
  
    <div className="kw-auth-atmosphere" aria-hidden="true">
      <span className="kw-auth-orb kw-auth-orb-one" />
      <span className="kw-auth-orb kw-auth-orb-two" />
      <span className="kw-auth-firefly kw-auth-firefly-one" />
      <span className="kw-auth-firefly kw-auth-firefly-two" />
      <span className="kw-auth-firefly kw-auth-firefly-three" />
    </div>

    <aside className="kw-auth-story">
      <span className="kw-auth-story-kicker">A NEIGHBOURHOOD LIBRARY</span>
      <div className="kw-auth-story-arch" aria-hidden="true">
        <span className="kw-auth-book-spine kw-auth-book-one" />
        <span className="kw-auth-book-spine kw-auth-book-two" />
        <span className="kw-auth-book-spine kw-auth-book-three" />
      </div>
      <div className="kw-auth-story-copy">
        <span className="kw-auth-story-overline">A little note from the woods</span>
        <h2>{storyTitle}</h2>
        <p>{storyCopy}</p>
        <span className="kw-auth-story-signoff">Good stories are meant to be shared.</span>
      </div>
    </aside>

    <div className="kw-auth-card">
      <span className="kw-auth-eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      <p className="kw-auth-intro">{intro}</p>
      {children}
      <div className="kw-auth-footer">{footer}</div>
    </div>

    <p className="kw-auth-whisper">A quiet corner for readers, and room for every story.</p>
  </section>;
}

export function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(f.email, f.password);
      nav(loc.state?.from || '/library');
    } catch (er) {
      setError(er?.code === 'ERR_NETWORK'
        ? 'The library service is not connected. You can preview this page, but signing in needs the backend.'
        : errMsg(er));
    } finally {
      setBusy(false);
    }
  };

  return <AuthFrame
    eyebrow="Welcome back"
    title="Your story continues here"
    intro="Show us your library card and step back inside."
    storyTitle="Some stories feel like coming home."
    storyCopy="Your next read might be resting on a neighbour's shelf. Sign in to find it, borrow it, and pass a favourite along."
    footer={<>New to Khwabnama? <Link to="/register">Join the library</Link></>}
  >
    <form onSubmit={submit} className="kw-auth-form">
      <label className="kw-auth-field">
        Email address
        <input type="email" required value={f.email}
          onChange={(e) => setF({ ...f, email: e.target.value })}
          autoComplete="email" placeholder="you@example.com" />
      </label>
      <label className="kw-auth-field">
        Password
        <input type="password" required value={f.password}
          onChange={(e) => setF({ ...f, password: e.target.value })}
          autoComplete="current-password" placeholder="Your password" />
      </label>
      <ErrorNote msg={error} />
      <button className="kw-auth-submit" disabled={busy}>
        {busy ? 'Opening the door…' : 'Step inside'}
      </button>
    </form>
  </AuthFrame>;
}

export function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const communities = useLoad(() => get('/communities/public'), []);
  const [f, setF] = useState({ name: '', email: '', password: '', communityId: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // The API can return an error or even an HTML fallback when the backend is offline.
  // Never assume its payload is an array: malformed data must not crash the route.
  const communityList = Array.isArray(communities.data) ? communities.data : [];
  const hasCommunities = communityList.length > 0;

  const submit = async (e) => {
    e.preventDefault();
    if (!hasCommunities) {
      setError('Neighbourhood choices are unavailable right now. Connect the library service to complete registration.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await register({ ...f, communityId: Number(f.communityId) });
      nav('/library');
    } catch (er) {
      setError(er?.code === 'ERR_NETWORK'
        ? 'The library service is not connected. Registration needs the backend.'
        : errMsg(er));
    } finally {
      setBusy(false);
    }
  };

  return <AuthFrame
    eyebrow="A place at the table"
    title="Make room for your stories"
    intro="Join your neighbourhood's little library and pass a good read along."
    storyTitle="There's always room for one more story."
    storyCopy="Bring a book you love, discover one you haven't met yet, and help good stories find their next home."
    footer={<>Already have a library card? <Link to="/login">Sign in</Link></>}
  >
    <form onSubmit={submit} className="kw-auth-form">
      <label className="kw-auth-field">
        Your name
        <input required value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })}
          maxLength={80} autoComplete="name" placeholder="How should we call you?" />
      </label>
      <label className="kw-auth-field">
        Email address
        <input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })}
          autoComplete="email" placeholder="you@example.com" />
      </label>
      <label className="kw-auth-field">
        Password <span className="kw-auth-hint">At least 8 characters</span>
        <input type="password" required minLength={8} value={f.password}
          onChange={(e) => setF({ ...f, password: e.target.value })}
          autoComplete="new-password" placeholder="Create a password" />
      </label>
      <label className="kw-auth-field">
        Your building or neighbourhood
        <select required value={f.communityId}
          onChange={(e) => setF({ ...f, communityId: e.target.value })}
          disabled={!hasCommunities}>
          <option value="">
            {communities.loading ? 'Finding nearby communities…'
              : hasCommunities ? 'Choose your community…'
                : 'Community list unavailable'}
          </option>
          {communityList.map((c) => (
            <option key={c.id} value={c.id}>
              {c.path} ({String(c.level || '').toLowerCase()})
            </option>
          ))}
        </select>
      </label>
      {!hasCommunities && <p className="kw-auth-service-note" role="status">
        {communities.loading
          ? 'Checking which neighbourhoods are part of the library…'
          : 'The registration form is available to preview. Neighbourhood choices and account creation require the backend.'}
      </p>}
      <ErrorNote msg={error} />
      <button className="kw-auth-submit" disabled={busy || !hasCommunities}>
        {busy ? 'Preparing your library card…' : 'Join Khwabnama'}
      </button>
    </form>
  </AuthFrame>;
}
