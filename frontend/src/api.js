import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('bs_token');
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const onAuthPage = ['/login', '/register'].includes(window.location.pathname);
    if (err.response?.status === 401 && !onAuthPage) {
      localStorage.removeItem('bs_token');
      localStorage.removeItem('bs_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const errMsg = (e) => e?.response?.data?.error || e?.message || 'Something went wrong';

export const get = (url, params) => api.get(url, { params }).then((r) => r.data);
export const post = (url, body) => api.post(url, body).then((r) => r.data);
export const put = (url, body) => api.put(url, body).then((r) => r.data);
export const del = (url) => api.delete(url).then((r) => r.data);

/** POST/PUT multipart. `json` parts are sent as application/json blobs, `files` as repeated file parts. */
export function multipart(method, url, { json = {}, files = {}, fields = {} } = {}) {
  const fd = new FormData();
  Object.entries(json).forEach(([k, v]) => fd.append(k, new Blob([JSON.stringify(v)], { type: 'application/json' })));
  Object.entries(fields).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') fd.append(k, v); });
  Object.entries(files).forEach(([k, list]) => (Array.isArray(list) ? list : [list]).filter(Boolean).forEach((f) => fd.append(k, f)));
  return api({ method, url, data: fd }).then((r) => r.data);
}
export default api;
