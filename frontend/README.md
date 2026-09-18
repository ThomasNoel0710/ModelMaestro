# ModelMaestro frontend

React + Vite workspace matching the approved light Material-inspired mockup. The wordmark has no logo; composer controls are deliberately compact.

## Run

From the project root, run `scripts\start-frontend.cmd` (or the PowerShell equivalent). It installs dependencies on first use and starts http://localhost:5173.

Alternatively, in `frontend`: `npm ci`, then `npm run dev`. Production build: `npm run build`.

## Modes

- **Demo mode** (default): browser-local model configurations and conversation history, illustrative responses, attachment filename previews. No provider calls or uploads. Prices are example data.
- **Local backend**: select in Settings. Start Spring Boot on port 8080 separately. Vite proxies `/api` to the backend, so no backend CORS changes are necessary. Create/enable models in Models & connections, then send an objective to create and execute a Run. A Supervisor configuration must be enabled.

The backend currently uses fake AI clients. Its execution request is synchronous; the UI shows a waiting indicator, not fabricated intermediate events. It reads actual plan/result/cost fields after the response. Refresh status is available if an execute request fails or disconnects; execution is never automatically retried.

Sessions are frontend-only and stored in localStorage. Every backend message creates an independent Run: previous messages are not sent as context. Attachments are disabled in backend mode; in demo mode only filenames and sizes are stored. No file content is uploaded or read. Model credentials are not collected. Budget display is not a guarantee of hard enforcement.

The inspector distinguishes planned assignments from the currently generic backend Worker call. Router execution, per-task status, token usage, cancellation, and server session storage remain backend follow-up work.

## Deployment

`dist/` is a static build. Production hosting must serve it and reverse-proxy `/api` to Spring Boot. The development proxy applies only to `npm run dev`.

## Privacy

Browser storage contains conversation text and attachment names; use this local learning version only with appropriate data. There is no authentication. The optional Google Fonts stylesheet falls back to Arial when offline.
