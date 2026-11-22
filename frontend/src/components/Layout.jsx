import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { Fireflies, Vines } from './Decor.jsx';

const SIGNS = [
  ['/library', 'Library'],
  ['/marketplace', 'Marketplace'],
  ['/exchanges', 'My Exchanges'],
  ['/shelf', 'My Shelf'],
  ['/rooms', 'Book Clubs'],
  ['/reading-rooms', 'Reading Rooms']
];

export default function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const location = useLocation();
  const isHome = location.pathname === '/';
  const isAuthPage = ['/login', '/register'].includes(location.pathname);
  const [open, setOpen] = useState(false);
  const signs = user?.role === 'ADMIN' ? [...SIGNS, ['/admin', 'Admin']] : SIGNS;
  const hideGuestHomeHeader = isHome && !user;

  return <div className={`app ${isHome ? 'app-home' : ''} ${hideGuestHomeHeader ? 'app-woodland-guest' : ''} ${isAuthPage ? 'app-auth' : ''}`}>
    {!isHome && <Fireflies />}
    {!hideGuestHomeHeader && <header className={`header ${isHome ? 'header-home' : ''} ${isAuthPage ? 'header-auth' : ''}`}>
      {!isHome && !isAuthPage && <><Vines side="left" /><Vines side="right" /></>}
      <div className="header-inner">
        <Link to="/" className="brand">
          <span className="logo" aria-hidden="true">✧</span>
          <span><b>Khwabnama</b><small>a little world between the trees</small></span>
        </Link>
        {user && <>
          <button className="btn ghost small menu-toggle" onClick={() => setOpen(!open)} aria-label="Menu">☰ Paths</button>
          <ul className={`signpost ${open ? 'open' : ''}`} onClick={() => setOpen(false)}>
            {signs.map(([to, text]) => <li key={to} className="sign-wrap">
              <NavLink to={to} className={({ isActive }) => `sign ${isActive ? 'active' : ''}`}>{text}</NavLink>
            </li>)}
          </ul>
          <div className="userbox">
            <Link to={`/profile/${user.id}`} className="avatar" title="My profile">{user.name?.[0]?.toUpperCase()}</Link>
            <button className="btn ghost small" onClick={() => { logout(); nav('/'); }}>Leave</button>
          </div>
        </>}
        {!user && !isAuthPage && <div className="userbox" style={{ marginLeft: 'auto' }}>
          <Link to="/login" className="btn ghost small">Sign in</Link>
          <Link to="/register" className="btn small">Join the library</Link>
        </div>}
      </div>
    </header>}
    <main className={`main ${isHome ? 'main-home' : ''}`}><Outlet /></main>
    {!isHome && <footer className="footer">Read a little. Dream a little. Let the stories wander.</footer>}
  </div>;
}
