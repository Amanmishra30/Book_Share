import { createContext, useCallback, useContext, useState } from 'react';

const Ctx = createContext({ ok: () => {}, err: () => {} });
export const useToast = () => useContext(Ctx);

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const push = useCallback((text, kind) => {
    const id = Math.random().toString(36).slice(2);
    setItems((l) => [...l, { id, text, kind }]);
    setTimeout(() => setItems((l) => l.filter((x) => x.id !== id)), 4200);
  }, []);
  const api = { ok: (t) => push(t, 'ok'), err: (t) => push(t, 'err') };
  return (
    <Ctx.Provider value={api}>
      {children}
      <div className="toasts" role="status">
        {items.map((t) => <div key={t.id} className={`toast ${t.kind === 'err' ? 'err' : ''}`}>{t.text}</div>)}
      </div>
    </Ctx.Provider>
  );
}
