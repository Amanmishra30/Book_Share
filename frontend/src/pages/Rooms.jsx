import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { errMsg, get, post } from '../api.js';
import { useLoad } from '../hooks.js';
import ChatBox from '../components/ChatBox.jsx';
import { useToast } from '../components/Toast.jsx';
import { Empty, ErrorNote, Loading, Modal, Pill } from '../components/ui.jsx';
import { fmtDateTime, label } from '../util.js';

export function Rooms() {
  const toast = useToast();
  const rooms = useLoad(() => get('/rooms'), []);
  const [form, setForm] = useState(null);
  const [error, setError] = useState('');

  const create = async (e) => {
    e.preventDefault();
    try {
      await post('/rooms', { name: form.name, topic: form.topic, type: form.type, scheduledAt: form.type === 'BOOK_CLUB' && form.scheduledAt ? new Date(form.scheduledAt).toISOString() : null });
      toast.ok('Room opened.'); setForm(null); rooms.reload();
    } catch (er) { setError(errMsg(er)); }
  };

  return (
    <>
      <div className="row between">
        <div><span className="hand">Gather round</span><h1 style={{ fontSize: '2.2rem' }}>Chatrooms & book clubs</h1><p className="muted">Rooms are private to your community. Neighbours from other buildings cannot see them.</p></div>
        <button className="btn honey" onClick={() => { setError(''); setForm({ name: '', topic: '', type: 'CHAT', scheduledAt: '' }); }}>+ Open a room</button>
      </div>
      <ErrorNote msg={rooms.error} />
      {rooms.loading ? <Loading /> : rooms.data.length === 0 ? <Empty icon="🕯️" title="No rooms lit yet">Open a genre room or a book club for your community.</Empty> : (
        <div className="grid wide">
          {rooms.data.map((r) => (
            <Link key={r.id} to={`/rooms/${r.id}`} className="card stitched" style={{ textDecoration: 'none', color: 'inherit' }}>
              <div className="row"><Pill tone={r.type === 'BOOK_CLUB' ? 'lilac' : 'sky'}>{r.type === 'BOOK_CLUB' ? 'Book club' : 'Chatroom'}</Pill>{r.topic && <Pill tone="honey">{r.topic}</Pill>}</div>
              <h3 style={{ marginTop: '.5rem' }}>{r.name}</h3>
              <span className="small muted">opened by {r.createdBy?.name}{r.scheduledAt ? ` · meets ${fmtDateTime(r.scheduledAt)}` : ''}</span>
            </Link>
          ))}
        </div>
      )}
      {form && (
        <Modal title="Open a room" onClose={() => setForm(null)}>
          <form onSubmit={create} className="stack">
            <label className="field">Name<input required maxLength={120} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
            <label className="field">Genre or topic tag<input maxLength={80} value={form.topic} onChange={(e) => setForm({ ...form, topic: e.target.value })} placeholder="e.g. Fantasy, Poetry, General" /></label>
            <label className="field">Kind of room<select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}><option value="CHAT">Open chatroom</option><option value="BOOK_CLUB">Book club (time-boxed discussion)</option></select></label>
            {form.type === 'BOOK_CLUB' && <label className="field">Discussion time<input type="datetime-local" value={form.scheduledAt} onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })} /></label>}
            <ErrorNote msg={error} />
            <div><button className="btn">Light the lantern</button></div>
          </form>
        </Modal>
      )}
    </>
  );
}

export function RoomChat() {
  const { id } = useParams();
  const { user } = useAuth();
  const room = useLoad(() => get(`/rooms/${id}`), [id]);
  if (room.loading) return <Loading />;
  if (room.error) return <ErrorNote msg={room.error} />;
  const r = room.data;
  return (
    <>
      <p><Link to="/rooms">← All rooms</Link></p>
      <div className="row"><h1 style={{ marginBottom: 0 }}>{r.name}</h1><Pill tone={r.type === 'BOOK_CLUB' ? 'lilac' : 'sky'}>{label(r.type)}</Pill>{r.topic && <Pill tone="honey">{r.topic}</Pill>}</div>
      {r.scheduledAt && <p className="note" style={{ margin: '.8rem 0' }}>This book club meets on <b>{fmtDateTime(r.scheduledAt)}</b>.</p>}
      <div style={{ marginTop: '1rem' }}>
        <ChatBox meId={user.id} topic={`/topic/room.${id}`} loadHistory={() => get(`/chat/room/${id}/messages`)} send={(content) => post(`/chat/room/${id}/messages`, { content })} />
      </div>
    </>
  );
}
