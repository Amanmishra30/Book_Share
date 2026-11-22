import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../authContext.jsx';
import { del, errMsg, get, post } from '../api.js';
import { useLoad, useReadingCall } from '../hooks.js';
import ChatBox from '../components/ChatBox.jsx';
import { useToast } from '../components/Toast.jsx';
import { Empty, ErrorNote, Loading, Modal, Pill } from '../components/ui.jsx';
import { fmtDateTime, toLocalInput } from '../util.js';

export function ReadingRooms() {
  const { user } = useAuth();
  const toast = useToast();
  const nav = useNavigate();
  const rooms = useLoad(() => get('/reading-rooms'), []);
  const [form, setForm] = useState(null);
  const [error, setError] = useState('');

  const create = async (e) => {
    e.preventDefault();
    try {
      const r = await post('/reading-rooms', { title: form.title, description: form.description, scheduledTime: new Date(form.scheduledTime).toISOString(), maxParticipants: Number(form.maxParticipants) });
      toast.ok('Reading room scheduled.'); nav(`/reading-rooms/${r.id}`);
    } catch (er) { setError(errMsg(er)); }
  };

  return (
    <>
      <div className="row between">
        <div><span className="hand">Quiet company</span><h1 style={{ fontSize: '2.2rem' }}>Reading rooms</h1><p className="muted">Schedule a shared reading session with text chat and optional live audio or video.</p></div>
        <button className="btn honey" onClick={() => { setError(''); setForm({ title: '', description: '', scheduledTime: toLocalInput(new Date(Date.now() + 86400000)), maxParticipants: 6 }); }}>+ Schedule a session</button>
      </div>
      <ErrorNote msg={rooms.error} />
      {rooms.loading ? <Loading /> : rooms.data.length === 0 ? <Empty icon="🫖" title="No sessions yet">Schedule one and invite your neighbours.</Empty> : (
        <div className="grid wide">
          {rooms.data.map((r) => {
            const joined = r.participants.some((p) => p.id === user.id);
            return (
              <div key={r.id} className="card stitched stack gap-s">
                <div className="row"><Pill tone="lilac">{fmtDateTime(r.scheduledTime)}</Pill><Pill tone={r.participants.length >= r.maxParticipants ? 'rose' : 'moss'}>{r.participants.length}/{r.maxParticipants} seats</Pill></div>
                <h3 style={{ marginBottom: 0 }}>{r.title}</h3>
                {r.description && <p className="small muted">{r.description}</p>}
                <span className="small muted">hosted by {r.host.name}</span>
                <div className="row">
                  <Link className="btn small" to={`/reading-rooms/${r.id}`}>{joined ? 'Enter room' : 'Join'}</Link>
                  {r.host.id === user.id && <button className="btn danger small" onClick={async () => { if (window.confirm('Close this room?')) { await del(`/reading-rooms/${r.id}`); rooms.reload(); } }}>Close</button>}
                </div>
              </div>
            );
          })}
        </div>
      )}
      {form && (
        <Modal title="Schedule a reading session" onClose={() => setForm(null)}>
          <form onSubmit={create} className="stack">
            <label className="field">Title<input required maxLength={150} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
            <label className="field">Description<textarea maxLength={1000} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
            <div className="form-grid">
              <label className="field">When<input type="datetime-local" required value={form.scheduledTime} onChange={(e) => setForm({ ...form, scheduledTime: e.target.value })} /></label>
              <label className="field">Max readers (2 to 12)<input type="number" min="2" max="12" value={form.maxParticipants} onChange={(e) => setForm({ ...form, maxParticipants: e.target.value })} /></label>
            </div>
            <ErrorNote msg={error} />
            <div><button className="btn">Schedule it</button></div>
          </form>
        </Modal>
      )}
    </>
  );
}

function VideoTile({ stream, muted, name }) {
  const ref = useRef(null);
  useEffect(() => { if (ref.current) ref.current.srcObject = stream; }, [stream]);
  return <div><video ref={ref} autoPlay playsInline muted={muted} aria-label={name} /><div className="small muted center">{name}</div></div>;
}

export function ReadingRoomLive() {
  const { id } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();
  const room = useLoad(() => post(`/reading-rooms/${id}/join`), [id]);   // joining is idempotent
  const [mode, setMode] = useState('off');   // off | audio | video
  const call = useReadingCall({ roomId: Number(id), meId: user.id, active: mode !== 'off', video: mode === 'video' });

  useEffect(() => {
    const t = setInterval(() => get(`/reading-rooms/${id}`).then(room.setData).catch(() => {}), 8000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (room.loading && !room.data) return <Loading />;
  if (room.error) return <><p><Link to="/reading-rooms">← Reading rooms</Link></p><ErrorNote msg={room.error} /></>;
  const r = room.data;
  const nameOf = (uid) => r.participants.find((p) => p.id === Number(uid))?.name || 'Reader';

  return (
    <>
      <p><Link to="/reading-rooms">← Reading rooms</Link></p>
      <div className="row between">
        <div><h1 style={{ marginBottom: 0 }}>{r.title}</h1><p className="muted">{fmtDateTime(r.scheduledTime)} · hosted by {r.host.name}</p></div>
        <button className="btn ghost" onClick={async () => { setMode('off'); await post(`/reading-rooms/${id}/leave`).catch(() => {}); nav('/reading-rooms'); }}>Leave the room</button>
      </div>
      {r.description && <p>{r.description}</p>}
      <div className="row" style={{ margin: '.5rem 0 1rem' }}>
        {r.participants.map((p) => <Pill key={p.id} tone="moss">{p.name}</Pill>)}
        <span className="small muted">{r.participants.length}/{r.maxParticipants} seats</span>
      </div>

      <div className="card stack" style={{ marginBottom: '1.2rem' }}>
        <div className="row between">
          <div><h3 style={{ marginBottom: 0 }}>Live audio & video <Pill tone="muted">optional</Pill></h3><span className="small muted">Peer-to-peer WebRTC. Your browser will ask for microphone or camera access.</span></div>
          <div className="row">
            {mode === 'off' ? <><button className="btn small" onClick={() => setMode('audio')}>🎙️ Join with audio</button><button className="btn honey small" onClick={() => setMode('video')}>📹 Join with video</button></>
              : <button className="btn danger small" onClick={() => setMode('off')}>Stop live call</button>}
          </div>
        </div>
        <ErrorNote msg={call.error} />
        {mode !== 'off' && (
          <div className="videos">
            {call.localStream && <VideoTile stream={call.localStream} muted name="You" />}
            {Object.entries(call.remote).map(([uid, st]) => <VideoTile key={uid} stream={st} name={nameOf(uid)} />)}
          </div>
        )}
      </div>

      <h2>Room chat</h2>
      <ChatBox meId={user.id} topic={`/topic/reading.${id}`} loadHistory={() => get(`/chat/reading/${id}/messages`)} send={(content) => post(`/chat/reading/${id}/messages`, { content })} />
    </>
  );
}
