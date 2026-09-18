export async function request(path, { method = 'GET', body } = {}) {
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!response.ok) {
    throw new Error(data?.detail || data?.message || `Request failed (${response.status}). Check that the backend is running on port 8080.`);
  }
  if (data === null) throw new Error('The backend returned an empty or invalid response. Check the server connection.');
  return data;
}

export const api = {
  models: () => request('/models'),
  createModel: (body) => request('/models', { method: 'POST', body }),
  enableModel: (id, enabled) => request(`/models/${id}/enabled`, { method: 'PATCH', body: { enabled } }),
  createRun: (body) => request('/runs', { method: 'POST', body }),
  executeRun: (id) => request(`/runs/${id}/execute`, { method: 'POST' }),
  getRun: (id) => request(`/runs/${id}`),
};

export function parsePlan(value) {
  try {
    const plan = JSON.parse(value);
    return Array.isArray(plan?.tasks) ? plan : null;
  } catch { return null; }
}

export function money(micros = 0) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 4 }).format(micros / 1_000_000);
}
