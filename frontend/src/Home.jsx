import { Link } from 'react-router-dom';
import { useAuth } from './authContext.jsx';
import { get } from './api.js';
import { useLoad } from './hooks.js';
import { BookCard } from './components/ui.jsx';
import woodlandArch from './assets/woodland-library-arch.png';
import './khwabnama-threshold-v4.css';

function FireflyField() {
  const lights = Array.from({ length: 18 }, (_, i) => ({
    left: `${(i * 43 + 9) % 96}%`, top: `${(i * 31 + 12) % 88}%`,
    delay: `${-(i * 0.57) % 7}s`, duration: `${4.8 + (i % 5) * 1.1}s`,
  }));
  return <div className="kw-firefly-field" aria-hidden="true">{lights.map((light, i) => <i key={i} style={{ '--left': light.left, '--top': light.top, '--delay': light.delay, '--duration': light.duration }} />)}</div>;
}
function Butterfly({ className = '' }) {
  return <span className={`kw-butterfly ${className}`} aria-hidden="true"><i /><b /></span>;
}
export default function Home() {
  const { user } = useAuth();
  const recs = useLoad(() => (user ? get('/recommendations') : Promise.resolve([])), [user?.id]);
  const fresh = useLoad(() => (user ? get('/books', { scope: 'parent' }) : Promise.resolve([])), [user?.id]);
  return (
    <div className="khwab-home">
      <section className="khwab-threshold" aria-label="Welcome to Khwabnama, a library hidden in the woods">
        <img className="khwab-threshold-art" src={woodlandArch} alt="A hand-painted woodland library arch covered in flowers and vines, with open and closed books hanging above it" />
        <div className="khwab-art-wash" aria-hidden="true" />
        <FireflyField />
        <Butterfly className="kw-butterfly-one" /><Butterfly className="kw-butterfly-two" /><Butterfly className="kw-butterfly-three" />
        <div className="khwab-threshold-copy">
          <span className="khwab-eyebrow">A little library, hidden in the woods</span>
          <h1>Khwabnama</h1>
          <p>A place for stories to find their way home.</p>
          {!user && <nav className="khwab-entry-signs" aria-label="Enter the library"><Link to="/login" className="khwab-sign khwab-sign-login">Sign in</Link><Link to="/register" className="khwab-sign khwab-sign-join">Join the library</Link></nav>}
          {user && <Link to="/library" className="khwab-sign khwab-sign-join khwab-enter-library">Enter the library</Link>}
        </div>
        <span className="khwab-ground-whisper" aria-hidden="true">Come in. There's room for one more story.</span>
        <span className="khwab-sr-only">Discover, borrow and share books with your neighbours.</span>
      </section>
      {user && <section className="home-member-shelves khwab-member-shelves">
        {recs.data?.length > 0 && <><div className="section-title"><h2>Stories you might like</h2><span className="hand">from readers nearby</span></div><div className="grid">{recs.data.map((b) => <BookCard key={b.id} book={b} />)}</div></>}
        {fresh.data?.length > 0 && <><div className="section-title"><h2>New on the shelves</h2><span className="hand">recently shared</span></div><div className="grid">{fresh.data.slice(0, 8).map((b) => <BookCard key={b.id} book={b} />)}</div></>}
      </section>}
    </div>
  );
}
