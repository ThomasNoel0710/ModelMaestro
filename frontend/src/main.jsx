import React, { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Plus, Search, MessageSquare, Settings, PanelRight, PanelLeft, Paperclip, ArrowUp, X, Check, ChevronRight, FileText, Copy, CheckCheck, MoreHorizontal, Database, RefreshCw, AlertCircle, Layers, CircleHelp, Trash2 } from 'lucide-react';
import { api, money, parsePlan } from './api';
import './style.css';

const STORE = 'modelmaestro-ui-v1';
const uid = () => crypto.randomUUID();
const DEMO_MODELS = [
  { id: 'demo-claude', displayName: 'Claude', modelId: 'claude-demo', protocol: 'ANTHROPIC', capabilityScore: 10, description: 'Complex reasoning, planning, and final review.', inputCostMicrosPerMillionTokens: 3000000, outputCostMicrosPerMillionTokens: 15000000, enabled: true },
  { id: 'demo-deepseek', displayName: 'DeepSeek', modelId: 'deepseek-demo', protocol: 'OPENAI_COMPATIBLE', capabilityScore: 7, description: 'Routine implementation and code analysis.', inputCostMicrosPerMillionTokens: 300000, outputCostMicrosPerMillionTokens: 1000000, enabled: true },
  { id: 'demo-minimax', displayName: 'MiniMax', modelId: 'minimax-demo', protocol: 'OPENAI_COMPATIBLE', capabilityScore: 6, description: 'Summarization and general-purpose tasks.', inputCostMicrosPerMillionTokens: 200000, outputCostMicrosPerMillionTokens: 800000, enabled: true },
];
function demoRun(objective, supervisor = 'demo-claude') {
  return { id: `demo-${uid()}`, objective, status: 'COMPLETED', supervisorConfigId: supervisor, budgetMicros: 1000000, spentMicros: 40000,
    plan: JSON.stringify({ summary: 'Inspect the project, review service boundaries, and combine the findings.', tasks: [
      { instruction: 'Inspect project structure', assignedModelConfigId: 'demo-deepseek', reason: 'Suitable for routine code analysis at lower cost.' },
      { instruction: 'Review service boundaries', assignedModelConfigId: 'demo-deepseek', reason: 'A focused analysis task suitable for this model.' },
    ] }), work: 'Demo: project structure and service boundaries reviewed.', finalResult: 'Demo review completed. These findings illustrate the interface, not an analysis of an uploaded repository.' };
}
function seedSession() {
  const run = demoRun('Review this Spring Boot project and suggest improvements.');
  return { id: uid(), title: 'Repository review', mode: 'demo', updatedAt: Date.now(), messages: [
    { id: uid(), role: 'user', content: run.objective, files: [{ name: 'backend.zip', size: 131072 }] },
    { id: uid(), role: 'assistant', content: run.finalResult, demo: true, run },
  ] };
}
function loadState() {
  try { const value = JSON.parse(localStorage.getItem(STORE)); if (value?.version === 1 && Array.isArray(value.sessions)) return value; } catch { /* Start clean when storage is unavailable or invalid. */ }
  const session = seedSession();
  return { version: 1, sessions: [session], activeId: session.id, mode: 'demo', models: DEMO_MODELS };
}
const initial = loadState();
function IconButton({ label, children, ...props }) { return <button type="button" className="icon-button" aria-label={label} title={label} {...props}>{children}</button>; }
function App() {
  const [sessions, setSessions] = useState(initial.sessions);
  const [activeId, setActiveId] = useState(initial.activeId);
  const [mode, setMode] = useState(initial.mode);
  const [demoModels, setDemoModels] = useState(initial.models || DEMO_MODELS);
  const [serverModels, setServerModels] = useState([]);
  const [page, setPage] = useState('chat');
  const [query, setQuery] = useState('');
  const [inspector, setInspector] = useState(() => window.innerWidth > 980);
  const [sidebar, setSidebar] = useState(false);
  const [tab, setTab] = useState('Activity');
  const [input, setInput] = useState('');
  const [files, setFiles] = useState([]);
  const [supervisor, setSupervisor] = useState('');
  const [budget, setBudget] = useState('1');
  const [busy, setBusy] = useState(false);
  const [loadingModels, setLoadingModels] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modal, setModal] = useState(false);
  const [menu, setMenu] = useState(false);
  const [selectedRun, setSelectedRun] = useState(null);
  const [selectedTask, setSelectedTask] = useState(0);
  const [savingModel, setSavingModel] = useState(false);
  const fileInput = useRef(null), bottom = useRef(null), sendLock = useRef(false), firstScroll = useRef(true);
  const models = mode === 'demo' ? demoModels : serverModels;
  const session = sessions.find(s => s.id === activeId && s.mode === mode);
  const latestRun = session?.messages.filter(m => m.run).at(-1)?.run;
  const run = selectedRun || latestRun;
  const plan = parsePlan(run?.plan);
  const modelName = id => models.find(m => m.id === id)?.displayName || 'Configured model';

  useEffect(() => { try { localStorage.setItem(STORE, JSON.stringify({ version: 1, sessions, activeId, mode, models: demoModels })); } catch { setNotice('Browser storage is full or unavailable. New changes may not survive a refresh.'); } }, [sessions, activeId, mode, demoModels]);
  useEffect(() => { if (!models.some(m => m.id === supervisor && m.enabled)) setSupervisor(models.find(m => m.enabled)?.id || ''); }, [models, supervisor]);
  useEffect(() => { if (firstScroll.current) { firstScroll.current = false; return; } bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }); }, [session?.messages.length, busy]);
  useEffect(() => { if (mode === 'backend') refreshModels(); }, [mode]);
  useEffect(() => { if (!notice) return; const timer = setTimeout(() => setNotice(''), 5000); return () => clearTimeout(timer); }, [notice]);
  async function refreshModels() { setLoadingModels(true); setError(''); try { setServerModels(await api.models()); } catch (e) { setError(e.message); } finally { setLoadingModels(false); } }
  function openSession(id) { setActiveId(id); setPage('chat'); setSelectedRun(null); setSelectedTask(0); setFiles([]); setInput(''); setSidebar(false); setMenu(false); setError(''); }
  function newSession() { const s = { id: uid(), mode, title: 'New session', updatedAt: Date.now(), messages: [] }; setSessions(prev => [s, ...prev]); openSession(s.id); }
  function switchMode(value) { if (busy || value === mode) return; setMode(value); setSelectedRun(null); setSelectedTask(0); setInput(''); setFiles([]); setError(''); setActiveId(sessions.find(s => s.mode === value)?.id || null); }
  function updateSession(id, transform) { setSessions(prev => prev.map(s => s.id === id ? transform(s) : s)); }
  async function send(event) {
    event.preventDefault();
    if (!input.trim() || sendLock.current) return;
    if (!supervisor) { setError('Add and enable a model before starting a run.'); return; }
    sendLock.current = true; setBusy(true); setError(''); setSelectedRun(null);
    const objective = input.trim(), sessionId = session?.id || uid(), currentMode = mode;
    const message = { id: uid(), role: 'user', content: objective, files: files.map(f => ({ name: f.name, size: f.size })) };
    if (!session) setSessions(prev => [{ id: sessionId, mode, title: objective.slice(0, 36), updatedAt: Date.now(), messages: [message] }, ...prev]);
    else updateSession(sessionId, s => ({ ...s, title: s.messages.length ? s.title : objective.slice(0, 36), updatedAt: Date.now(), messages: [...s.messages, message] }));
    setActiveId(sessionId); setInput(''); setFiles([]);
    let created;
    try {
      let result;
      if (currentMode === 'demo') {
        await new Promise(resolve => setTimeout(resolve, 850));
        result = { ...demoRun(objective, supervisor), budgetMicros: Number(budget) * 1000000, spentMicros: Math.min(40000, Number(budget) * 1000000) };
      } else {
        created = await api.createRun({ objective, budgetMicros: Math.round(Number(budget) * 1000000), supervisorConfigId: supervisor });
        setSelectedRun(created);
        result = await api.executeRun(created.id);
      }
      updateSession(sessionId, s => ({ ...s, updatedAt: Date.now(), messages: [...s.messages, { id: uid(), role: 'assistant', content: result.finalResult || result.work || 'Run completed without a final response.', demo: currentMode === 'demo', run: result }] }));
      setSelectedRun(result);
    } catch (e) {
      let failed = created;
      if (created) { try { failed = await api.getRun(created.id); } catch { /* Keep known run ID for manual recovery. */ } }
      const message = `${e.message}${created ? ' The run may already have executed. Refresh its status before creating another run.' : ''}`;
      setError(message);
      updateSession(sessionId, s => ({ ...s, messages: [...s.messages, { id: uid(), role: 'assistant', content: message, error: true, run: failed }] }));
      if (failed) setSelectedRun(failed);
    } finally { setBusy(false); sendLock.current = false; }
  }
  async function refreshRun() { if (!run || mode === 'demo') return; try { const updated = await api.getRun(run.id); setSelectedRun(updated); updateSession(activeId, s => ({ ...s, messages: s.messages.map(m => m.run?.id === updated.id ? { ...m, run: updated, content: updated.finalResult || m.content, error: updated.status === 'FAILED' } : m) })); setNotice('Run status refreshed.'); } catch (e) { setError(e.message); } }
  async function toggleModel(model) { if (mode === 'demo') { setDemoModels(prev => prev.map(m => m.id === model.id ? { ...m, enabled: !m.enabled } : m)); return; } setSavingModel(true); try { const updated = await api.enableModel(model.id, !model.enabled); setServerModels(prev => prev.map(m => m.id === model.id ? updated : m)); } catch (e) { setError(e.message); } finally { setSavingModel(false); } }
  async function addModel(event) {
    event.preventDefault(); const form = event.currentTarget; const data = Object.fromEntries(new FormData(form));
    const body = { ...data, capabilityScore: Number(data.capabilityScore), inputCostMicrosPerMillionTokens: Math.round(Number(data.inputPrice) * 1000000), outputCostMicrosPerMillionTokens: Math.round(Number(data.outputPrice) * 1000000), enabled: true };
    delete body.inputPrice; delete body.outputPrice;
    setSavingModel(true); setError('');
    try { const result = mode === 'demo' ? { ...body, id: `demo-${uid()}` } : await api.createModel(body); if (mode === 'demo') setDemoModels(prev => [...prev, result]); else setServerModels(prev => [...prev, result]); setModal(false); setNotice('Model configuration added.'); } catch (e) { setError(e.message); } finally { setSavingModel(false); }
  }
  const filtered = sessions.filter(s => s.mode === mode && s.title.toLowerCase().includes(query.toLowerCase()));
  const isDone = run?.status === 'COMPLETED';
  const task = plan?.tasks[selectedTask] || plan?.tasks[0];
  const assigned = models.find(m => m.id === task?.assignedModelConfigId);

  return <div className="app-shell">
    {sidebar && <button className="scrim" aria-label="Close navigation" onClick={() => setSidebar(false)} />}
    <aside className={`sidebar ${sidebar ? 'mobile-open' : ''}`}>
      <a className="wordmark" href="#" onClick={e => { e.preventDefault(); setPage('chat'); }}>ModelMaestro</a>
      <button className="new-session" onClick={newSession} disabled={busy}><Plus size={22} />New session</button>
      <label className="search"><Search size={19} /><input aria-label="Search sessions" placeholder="Search sessions…" value={query} onChange={e => setQuery(e.target.value)} /></label>
      <div className="session-list"><div className="section-label">YOUR SESSIONS</div>{filtered.map(s => <button key={s.id} className={`session-row ${activeId === s.id && page === 'chat' ? 'selected' : ''}`} onClick={() => openSession(s.id)} disabled={busy}><MessageSquare size={18} /><span>{s.title}</span></button>)}{!filtered.length && <p className="sidebar-hint">{query ? 'No matching sessions' : 'Your next idea starts here.'}</p>}</div>
      <nav className="bottom-nav"><button className={page === 'models' ? 'selected' : ''} onClick={() => { setPage('models'); setSidebar(false); }}><Database size={19} />Models & connections</button><button className={page === 'settings' ? 'selected' : ''} onClick={() => { setPage('settings'); setSidebar(false); }}><Settings size={19} />Settings</button></nav>
      <div className="profile"><span className="avatar small">T</span><div>Personal workspace<small>Stored on this device</small></div><ChevronRight size={16} /></div>
    </aside>

    <main className="workspace">
      <header className="topbar"><IconButton label="Toggle navigation" className="icon-button mobile-nav" onClick={() => setSidebar(v => !v)}><PanelLeft size={21} /></IconButton><div className="heading"><h1>{page === 'chat' ? session?.title || 'New session' : page === 'models' ? 'Models & connections' : 'Settings'}</h1><p>{page === 'chat' ? 'Your ideas, coordinated.' : page === 'models' ? 'Choose the models. Define their strengths.' : 'Make this workspace yours.'}</p></div><div className="header-actions"><span className={`mode-label ${mode === 'backend' ? 'local' : ''}`}><i />{mode === 'demo' ? 'Demo mode' : 'Local backend'}</span>{page === 'chat' && <><div className="menu-wrap"><IconButton label="Session actions" onClick={() => setMenu(v => !v)}><MoreHorizontal size={22} /></IconButton>{menu && <div className="dropdown"><button disabled={!session || busy} onClick={() => { if (confirm('Delete this session from this browser? Backend runs will remain.')) { setSessions(prev => prev.filter(s => s.id !== activeId)); setActiveId(null); setSelectedRun(null); } setMenu(false); }}><Trash2 size={16} />Delete local session</button></div>}</div><button className={`execution-toggle ${inspector ? 'on' : ''}`} aria-pressed={inspector} onClick={() => setInspector(v => !v)}><PanelRight size={18} /><span>Execution</span></button></>}</div></header>
      {error && <div className="error-banner" role="alert"><AlertCircle size={18} /><span>{error}</span><IconButton label="Dismiss error" onClick={() => setError('')}><X size={16} /></IconButton></div>}
      {page === 'chat' ? <div className="chat-layout">
        <section className="conversation" aria-label="Conversation"><div className="messages">
          {(!session || !session.messages.length) && <div className="welcome"><span className="welcome-eyebrow">A LITTLE ORCHESTRATION. A LOT OF POSSIBILITY.</span><h2>What shall we work on?</h2><p>One goal. The right models, working together.</p><div className="suggestions">{[['Review a project', 'Review my project architecture and suggest improvements.'], ['Design an API', 'Design a REST API for a task management application.'], ['Explore an idea', 'Help me turn a product idea into an implementation plan.']].map(([title, prompt]) => <button key={title} onClick={() => setInput(prompt)}><Layers size={19} /><span>{title}</span><ChevronRight size={16} /></button>)}</div></div>}
          {session?.messages.map(message => <article key={message.id} className={`message ${message.role}`}><div className={`avatar ${message.role === 'assistant' ? 'ai' : ''}`}>{message.role === 'user' ? 'T' : 'M'}</div><div className="message-body"><div className="message-heading"><strong>{message.role === 'user' ? 'You' : 'ModelMaestro'}</strong><span>{message.demo ? 'Demo response' : message.role === 'assistant' ? 'Backend response' : ''}</span></div>{message.role === 'user' ? <><p className="user-content">{message.content}</p>{message.files?.map((f, i) => <span className="file-chip" key={i}><FileText size={17} />{f.name}<small>{Math.round(f.size / 1024)} KB · local preview</small></span>)}</> : <>{message.demo ? <DemoAnswer objective={message.run?.objective} /> : <p className={`response-content ${message.error ? 'error-text' : ''}`}>{message.content}</p>}<div className="message-tools"><IconButton label="Copy response" onClick={async () => { try { await navigator.clipboard.writeText(message.content); setNotice('Response copied.'); } catch { setNotice('Clipboard unavailable. Select the text to copy it.'); } }}><Copy size={15} /></IconButton>{message.run && <button className="text-button" onClick={() => { setSelectedRun(message.run); setSelectedTask(0); setInspector(true); }}>View execution<ChevronRight size={14} /></button>}</div></>}</div></article>)}
          {busy && <div className="running-note" role="status"><span className="spinner" />{mode === 'demo' ? 'Preparing a demo response…' : 'Waiting for the backend to finish Plan, Work, and Review…'}</div>}<div ref={bottom} />
        </div><div className="composer-area"><form className="composer" onSubmit={send}>{files.length > 0 && <div className="pending-files">{files.map((f, i) => <span className="file-chip" key={i}><FileText size={15} />{f.name}<IconButton label={`Remove ${f.name}`} onClick={() => setFiles(prev => prev.filter((_, j) => i !== j))}><X size={13} /></IconButton></span>)}</div>}<textarea aria-label="Message" placeholder="Ask a follow-up or attach a file…" value={input} maxLength={10000} onChange={e => setInput(e.target.value)} onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) { e.preventDefault(); e.currentTarget.form.requestSubmit(); } }} rows={2} disabled={busy} /><div className="composer-toolbar"><input ref={fileInput} type="file" hidden multiple onChange={e => { setFiles(prev => [...prev, ...Array.from(e.target.files)].slice(0, 5)); setNotice('Attachments are local previews only. Their contents are not uploaded or sent to models yet.'); e.target.value = ''; }} /><IconButton label={mode === 'demo' ? 'Attach local preview' : 'Attachments not supported by backend yet'} disabled={busy || mode === 'backend'} onClick={() => fileInput.current.click()}><Paperclip size={18} /></IconButton><label className="compact-select"><span>Supervisor</span><select aria-label="Supervisor model" value={supervisor} onChange={e => setSupervisor(e.target.value)} disabled={busy}><option value="" disabled>No enabled model</option>{models.filter(m => m.enabled).map(m => <option value={m.id} key={m.id}>{m.displayName}</option>)}</select></label><label className="compact-select budget"><Database size={13} /><select aria-label="Run budget" value={budget} onChange={e => setBudget(e.target.value)} disabled={busy}><option value="0.25">Budget $0.25</option><option value="1">Budget $1.00</option><option value="5">Budget $5.00</option><option value="10">Budget $10.00</option></select></label><button type="submit" className="send-button" aria-label="Send message" disabled={busy || !input.trim() || !supervisor}>{busy ? <span className="spinner" /> : <ArrowUp size={23} />}</button></div></form><p className="composer-footnote">{mode === 'demo' ? 'Demo responses · Attachments stay on your device' : 'Backend uses simulated AI clients · Each message creates a separate Run'}</p></div></section>
        {inspector && <aside className="inspector" aria-label="Execution details"><div className="inspector-heading"><h2>Execution details</h2><span>{mode === 'demo' ? 'Demo data' : 'Run data'}</span><IconButton label="Close execution details" onClick={() => setInspector(false)}><X size={16} /></IconButton></div><div className="tabs" role="tablist" aria-label="Execution detail tabs">{['Activity', 'Models', 'Usage'].map(t => <button key={t} role="tab" aria-selected={t === tab} onClick={() => setTab(t)}>{t}</button>)}</div><div className="inspector-content" role="tabpanel" aria-label={tab}>
          {!run ? <div className="empty-inspector"><Layers size={32} /><h3>A clear view of every run</h3><p>Send a message to see the plan, model assignments, and reported cost here.</p></div> : <><div className="run-status"><span>Run status</span><strong><i className={`status-dot ${isDone ? 'done' : run.status === 'FAILED' ? 'failed' : ''}`} />{busy ? 'Executing' : run.status.replaceAll('_', ' ').toLowerCase()}</strong><small>{mode === 'demo' ? 'Illustrative workflow · no model calls' : 'Synchronous execution · no live task events'}</small>{mode === 'backend' && <button className="text-button" onClick={refreshRun} disabled={busy}><RefreshCw size={13} />Refresh status</button>}</div>
          {tab === 'Activity' && <><div className="timeline">{[{ title: 'Plan', subtitle: `${modelName(run.supervisorConfigId)} · Supervisor`, text: plan ? `${plan.tasks.length} task${plan.tasks.length > 1 ? 's' : ''} planned` : 'Waiting for plan', complete: !!plan }, { title: 'Work', subtitle: mode === 'demo' ? 'DeepSeek · Worker' : 'Default fake-model · Worker', text: run.work ? mode === 'demo' ? 'Demo tasks completed' : 'One generic Worker call returned' : 'Waiting for work result', complete: !!run.work }, { title: 'Review', subtitle: `${modelName(run.supervisorConfigId)} · Supervisor`, text: isDone ? 'Final response ready' : 'Waiting for final review', complete: isDone }].map((step, i) => <div className="timeline-step" key={step.title}><span className={`step-circle ${step.complete ? 'complete' : ''}`}>{step.complete ? <Check size={18} /> : i + 1}</span><div><h3>{step.title}</h3><p>{step.subtitle}</p><small>{step.text}</small></div></div>)}</div>{plan && <div className="task-detail"><div className="section-label">PLANNED TASKS</div>{plan.tasks.map((t, i) => <button className={`task-option ${i === selectedTask ? 'active' : ''}`} key={i} onClick={() => setSelectedTask(i)}><span>{i + 1}</span>{t.instruction}<ChevronRight size={15} /></button>)}<h3>Model assignment</h3><p className="muted">User capability score</p><div className="spread"><span>{assigned?.displayName || task?.assignedModelConfigId}</span><strong>{assigned ? `${assigned.capabilityScore}/10` : '—'}</strong></div><h4>Reason</h4><p>{task?.reason}</p>{mode === 'backend' && <p className="note">Planned assignments are not yet used by the backend Worker. Router integration is the next backend step.</p>}</div>}</>}
          {tab === 'Models' && <div className="inspector-models"><p className="muted">Configured models in this workspace</p>{models.map(m => <div className="mini-model" key={m.id}><span className="model-letter">{m.displayName[0]}</span><div><strong>{m.displayName}</strong><small>{m.id === run.supervisorConfigId ? 'Selected Supervisor' : 'Available configuration'} · {m.capabilityScore}/10</small></div><i className={`status-dot ${m.enabled ? 'done' : 'off'}`} /></div>)}<button className="text-button" onClick={() => setPage('models')}>Manage models<ChevronRight size={15} /></button></div>}
          {tab === 'Usage' && <div className="usage-details"><h3>Run accounting</h3><div className="spread"><span>Reported cost</span><strong>{money(run.spentMicros)}</strong></div><div className="spread"><span>Configured budget</span><strong>{money(run.budgetMicros)}</strong></div><p className="note">{mode === 'demo' ? 'Illustrative amounts for the interface preview.' : 'Costs are simulated by the backend. Provider billing, token breakdowns, and hard budget enforcement are not available in this interface.'}</p><p className="muted run-id">Run ID<br />{run.id}</p></div>}
          </>}
        </div>{run && <div className="cost-footer"><div className="spread"><span>{mode === 'demo' ? 'Demo cost' : 'Reported cost'}</span><strong>{money(run.spentMicros)} <span>/ {money(run.budgetMicros)}</span></strong></div><div className="progress-track"><div style={{ width: `${Math.min(100, (run.spentMicros || 0) / (run.budgetMicros || 1) * 100)}%` }} /></div></div>}</aside>}
      </div> : page === 'models' ? <section className="management"><div className="management-intro"><div><h2>Your model library</h2><p>Give each model a role, a capability score, and a price.</p></div><div className="row">{mode === 'backend' && <IconButton label="Refresh models" disabled={loadingModels} onClick={refreshModels}><RefreshCw size={18} className={loadingModels ? 'spin' : ''} /></IconButton>}<button className="primary-button" onClick={() => setModal(true)}><Plus size={18} />Add model</button></div></div><div className="info-banner"><CircleHelp size={18} /><span>{mode === 'demo' ? 'These demo configurations are stored in your browser. Switch to Local backend in Settings to manage real database entries.' : 'Configurations are saved to your backend. All protocols currently use simulated clients; API keys are not collected.'}</span></div><div className="model-grid">{models.map(m => <article className="model-card" key={m.id}><div className="spread"><span className="model-letter">{m.displayName[0]}</span><button role="switch" aria-checked={m.enabled} aria-label={`Enable ${m.displayName}`} className={`switch ${m.enabled ? 'enabled' : ''}`} disabled={savingModel} onClick={() => toggleModel(m)}><span /></button></div><h3>{m.displayName}</h3><span className="protocol-label">{m.protocol.replaceAll('_', ' ')}</span><p>{m.description || 'No description provided.'}</p><div className="score-row"><span>Capability score</span><strong>{m.capabilityScore}<small> / 10</small></strong></div><div className="score-track"><span style={{ width: `${m.capabilityScore * 10}%` }} /></div><div className="model-prices"><span>Input / 1M tokens<strong>{money(m.inputCostMicrosPerMillionTokens)}</strong></span><span>Output / 1M tokens<strong>{money(m.outputCostMicrosPerMillionTokens)}</strong></span></div><div className="model-id">{m.modelId}</div></article>)}</div>{!models.length && !loadingModels && <div className="empty-state"><Database size={32} /><h3>No models configured</h3><p>Add your first model to start a Run.</p></div>}</section> : <section className="settings-page"><h2>Connection</h2><p>Choose where your workspace gets its data.</p><div className="settings-card">{[['demo', 'Demo mode', 'Explore the interface with illustrative responses. No backend required.'], ['backend', 'Local backend', 'Connect to Spring Boot on localhost:8080 through the development proxy.']].map(([value, title, desc]) => <label className="mode-option" key={value}><input type="radio" name="mode" checked={mode === value} disabled={busy} onChange={() => switchMode(value)} /><span><strong>{title}</strong><small>{desc}</small></span></label>)}</div><h2>Local workspace</h2><div className="settings-card privacy"><Database size={22} /><div><strong>Sessions stay in this browser</strong><p>Conversation history and attachment names use local storage. File contents are never persisted or uploaded. Backend mode sends only the message objective, budget, and Supervisor ID. Each message is an independent Run; chat history is not sent to the backend.</p><p>This version has no account sync or server-side session storage.</p></div></div></section>}
    </main>
    {notice && <div className="toast" role="status"><CheckCheck size={17} />{notice}</div>}
    {modal && <ModelDialog onClose={() => !savingModel && setModal(false)} onSubmit={addModel} saving={savingModel} mode={mode} error={error} />}
  </div>;
}

function DemoAnswer({ objective }) { return <><h2 className="answer-title">A clearer picture of your project</h2><p className="answer-lead">The demo team has reviewed the structure and service boundaries. Here’s how a coordinated response could look.</p><div className="inline-steps"><span><Check size={15} />Project structure</span><i /><span><Check size={15} />Service boundaries</span><i /><span><Check size={15} />Final review</span></div><div className="findings"><h3>Initial findings</h3><div className="finding"><span>1</span><div><h4>Keep orchestration responsibilities clear</h4><p>Let the execution service coordinate each phase, while repositories handle persistence and model clients handle provider communication.</p><span className="code-chip"><FileText size={14} />RunExecutionService.java</span></div></div><div className="finding"><span>2</span><div><h4>Validate model assignments before execution</h4><p>Check each selected model against the planning catalog and its current enabled status before creating the Worker client.</p></div></div></div><p className="demo-disclaimer">Illustrative response for “{objective}”. No files were analyzed or models contacted.</p></>; }

function ModelDialog({ onClose, onSubmit, saving, mode, error }) {
  const ref = useRef(null);
  useEffect(() => { ref.current.showModal(); }, []);
  return <dialog ref={ref} className="model-dialog" onCancel={e => { e.preventDefault(); onClose(); }}><form onSubmit={onSubmit}><div className="dialog-heading"><div><h2>Add a model</h2><p>{mode === 'demo' ? 'Create a local demo configuration.' : 'Save a model configuration to the backend.'}</p></div><IconButton label="Close dialog" disabled={saving} onClick={onClose}><X size={20} /></IconButton></div>{error && <p role="alert" className="error-text">{error}</p>}<label>Display name<input name="displayName" required maxLength={100} placeholder="e.g. My reasoning model" autoFocus /></label><div className="form-grid"><label>Protocol<select name="protocol"><option value="OPENAI_COMPATIBLE">OpenAI compatible</option><option value="ANTHROPIC">Anthropic</option><option value="GEMINI">Gemini</option></select></label><label>Capability score (1–10)<input name="capabilityScore" type="number" min="1" max="10" step="1" defaultValue="7" required /></label></div><label>Base URL<input name="baseUrl" type="url" placeholder="https://api.example.com/v1" required maxLength={2048} /></label><label>Provider model ID<input name="modelId" placeholder="provider-model-name" required maxLength={255} /></label><label>Description<textarea name="description" rows={2} placeholder="What is this model good at?" /></label><div className="form-grid"><label>Input USD / 1M tokens<input name="inputPrice" type="number" min="0" max="100000" step="0.000001" defaultValue="0" required /></label><label>Output USD / 1M tokens<input name="outputPrice" type="number" min="0" max="100000" step="0.000001" defaultValue="0" required /></label></div><p className="note">No credentials needed yet. Provider calls are simulated.</p><div className="dialog-actions"><button type="button" className="secondary-button" disabled={saving} onClick={onClose}>Cancel</button><button className="primary-button" disabled={saving}>{saving ? 'Saving…' : 'Add model'}</button></div></form></dialog>;
}

createRoot(document.getElementById('root')).render(<App />);
