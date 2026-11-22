import { useCallback, useEffect, useRef, useState } from 'react';
import { errMsg } from '../api.js';
import { useTopic } from '../hooks.js';
import { fmtTime } from '../util.js';
import { useToast } from './Toast.jsx';

/**
 * Real-time chat. History comes over REST, new messages arrive over STOMP (`topic`),
 * and sending goes through REST so the server can authorise and persist before broadcasting.
 */
export default function ChatBox({ meId, topic, loadHistory, send, placeholder = 'Write a message…', disabled = false }) {
  const toast = useToast();
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const end = useRef(null);
  const log = useRef(null);

  const add = useCallback((m) => setMsgs((l) => (l.some((x) => x.id === m.id) ? l : [...l, m])), []);

  useEffect(() => {
    let on = true;
    setMsgs([]);
    loadHistory()
      .then((d) => on && setMsgs((cur) => { const ids = new Set(d.map((x) => x.id)); return [...d, ...cur.filter((x) => !ids.has(x.id))]; }))
      .catch((e) => toast.err(errMsg(e)));
    return () => { on = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topic]);

  useTopic(topic, add);

  useEffect(() => { if (log.current) log.current.scrollTop = log.current.scrollHeight; }, [msgs]);

  const submit = (e) => {
    e.preventDefault();
    if (!text.trim() || busy) return;
    setBusy(true);
    send(text.trim()).then((m) => { add(m); setText(''); }).catch((er) => toast.err(errMsg(er))).finally(() => setBusy(false));
  };

  return (
    <div className="chat">
      <div className="chat-log" ref={log} aria-live="polite">
        {msgs.length === 0 && <div className="msg sys">No notes yet. Say hello! 🍂</div>}
        {msgs.map((m) => m.system
          ? <div key={m.id} className="msg sys">✦ {m.content}</div>
          : (
            <div key={m.id} className={`msg ${m.sender?.id === meId ? 'me' : ''}`}>
              {m.sender?.id !== meId && <small>{m.sender?.name}</small>}
              {m.content}
              <small style={{ textAlign: 'right' }}>{fmtTime(m.sentAt)}</small>
            </div>
          ))}
        <div ref={end} />
      </div>
      <form className="chat-form" onSubmit={submit}>
        <input value={text} onChange={(e) => setText(e.target.value)} placeholder={placeholder} maxLength={2000} disabled={disabled} aria-label="Message" />
        <button className="btn" disabled={busy || disabled || !text.trim()}>Send</button>
      </form>
    </div>
  );
}
