import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { errMsg } from './api.js';

export const wsUrl = () => `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;

export function newStompClient() {
  return new Client({
    brokerURL: wsUrl(),
    connectHeaders: { Authorization: `Bearer ${localStorage.getItem('bs_token')}` },
    reconnectDelay: 4000
  });
}

/** Loads data with a loader function; returns {data, loading, error, reload, setData}. */
export function useLoad(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const run = useCallback(() => {
    setState((s) => ({ ...s, loading: true, error: null }));
    return loader()
      .then((data) => setState({ data, loading: false, error: null }))
      .catch((e) => setState({ data: null, loading: false, error: errMsg(e) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  useEffect(() => { run(); }, [run]);
  return { ...state, reload: run, setData: (data) => setState((s) => ({ ...s, data })) };
}

/** Subscribes to a STOMP topic while the component is mounted. */
export function useTopic(destination, onMessage) {
  const cb = useRef(onMessage);
  cb.current = onMessage;
  useEffect(() => {
    if (!destination) return undefined;
    const client = newStompClient();
    client.onConnect = () => client.subscribe(destination, (m) => cb.current(JSON.parse(m.body)));
    client.activate();
    return () => { client.deactivate(); };
  }, [destination]);
}

const ICE = [{ urls: 'stun:stun.l.google.com:19302' }];

/**
 * Optional audio/video for a reading room: a small WebRTC mesh signalled over STOMP
 * (/app/reading/{id}/signal -> /topic/reading.{id}.signal). Fine for a handful of readers.
 * Rule: the participant with the smaller user id always sends the offer, which avoids offer collisions.
 */
export function useReadingCall({ roomId, meId, active, video }) {
  const [localStream, setLocalStream] = useState(null);
  const [remote, setRemote] = useState({});
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!active) return undefined;
    let stopped = false;
    let client = null;
    let local = null;
    const pcs = {};

    const send = (msg) => client && client.connected &&
      client.publish({ destination: `/app/reading/${roomId}/signal`, body: JSON.stringify(msg) });

    const closePeer = (id) => {
      if (pcs[id]) { pcs[id].close(); delete pcs[id]; }
      setRemote((r) => { const n = { ...r }; delete n[id]; return n; });
    };

    const makePeer = (id) => {
      if (pcs[id]) return pcs[id];
      const pc = new RTCPeerConnection({ iceServers: ICE });
      pcs[id] = pc;
      local.getTracks().forEach((t) => pc.addTrack(t, local));
      pc.onicecandidate = (e) => e.candidate && send({ type: 'ice', to: id, data: e.candidate });
      pc.ontrack = (e) => setRemote((r) => ({ ...r, [id]: e.streams[0] }));
      pc.onconnectionstatechange = () => { if (['failed', 'closed'].includes(pc.connectionState)) closePeer(id); };
      return pc;
    };

    const offerTo = async (id) => {
      if (pcs[id]) return;
      const pc = makePeer(id);
      await pc.setLocalDescription(await pc.createOffer());
      send({ type: 'offer', to: id, data: pc.localDescription });
    };

    const handle = async (m) => {
      if (m.from === meId) return;
      if (m.to && m.to !== meId) return;
      try {
        if (m.type === 'join') {
          if (meId < m.from) await offerTo(m.from);
          else send({ type: 'hello', to: m.from });
        } else if (m.type === 'hello') {
          if (meId < m.from) await offerTo(m.from);
        } else if (m.type === 'offer') {
          const pc = makePeer(m.from);
          await pc.setRemoteDescription(m.data);
          await pc.setLocalDescription(await pc.createAnswer());
          send({ type: 'answer', to: m.from, data: pc.localDescription });
        } else if (m.type === 'answer') {
          if (pcs[m.from]) await pcs[m.from].setRemoteDescription(m.data);
        } else if (m.type === 'ice') {
          if (pcs[m.from]) await pcs[m.from].addIceCandidate(m.data);
        } else if (m.type === 'leave') {
          closePeer(m.from);
        }
      } catch (e) { console.warn('signalling error', e); }
    };

    (async () => {
      try {
        local = await navigator.mediaDevices.getUserMedia({ audio: true, video });
        if (stopped) { local.getTracks().forEach((t) => t.stop()); return; }
        setLocalStream(local);
        client = newStompClient();
        client.onConnect = () => {
          client.subscribe(`/topic/reading.${roomId}.signal`, (f) => handle(JSON.parse(f.body)));
          send({ type: 'join' });
        };
        client.activate();
      } catch (e) {
        setError(errMsg(e) || 'Could not access the microphone or camera.');
      }
    })();

    return () => {
      stopped = true;
      send({ type: 'leave' });
      Object.keys(pcs).forEach((id) => pcs[id].close());
      if (local) local.getTracks().forEach((t) => t.stop());
      if (client) client.deactivate();
      setLocalStream(null);
      setRemote({});
    };
  }, [active, roomId, meId, video]);

  return { localStream, remote, error };
}
