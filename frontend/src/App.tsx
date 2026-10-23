import { useEffect, useState } from "react";
import type { Alarm, AlarmGroup, Incident, Recording, Telemetry } from "./types";
import { api } from "./api";
import { findIncident, groupAlarms, incidentName, nameOf, sortByTime } from "./data";
import { alarmNames, formatTime, ONLINE_TIMEOUT } from "./format";
import { usePolling } from "./usePolling";
import { useTheme } from "./useTheme";
import { SessionBar } from "./SessionBar";
import { SessionList } from "./SessionList";
import { AlarmList } from "./AlarmList";
import { CameraPanel } from "./CameraPanel";
import { RecordingList } from "./RecordingList";

const SESSIONS_INTERVAL = 5000;
const ALARMS_INTERVAL = 2000;
const LIVE_INTERVAL = 1000;
const ENDED_INTERVAL = 30000;
const RECORDINGS_INTERVAL = 10000;

function errorText(error: string, updatedAt: number) {
  if (updatedAt === 0) {
    return error;
  }

  return `${error} · ostatnio ${formatTime(updatedAt)}`;
}

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const sessions = usePolling<Incident[]>("/incidents", SESSIONS_INTERVAL);
  const alarms = usePolling<Alarm[]>("/alarms", ALARMS_INTERVAL);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  let incidents: Incident[] = [];

  if (sessions.data !== null) {
    incidents = sessions.data;
  }

  let active: Incident[] | null = null;

  if (sessions.data !== null) {
    active = [];

    for (const incident of incidents) {
      if (incident.status === "IN_PROGRESS") {
        active.push(incident);
      }
    }
  }

  const selected = findIncident(incidents, selectedId);
  let telemetryPath = null;
  let recordingsPath = null;
  let telemetryInterval = LIVE_INTERVAL;

  if (selected !== undefined) {
    telemetryPath = `/telemetry/incident/${selected.id}`;
    recordingsPath = `/videos/incident/${selected.id}`;
  }

  if (selected !== undefined && selected.status !== "IN_PROGRESS") {
    telemetryInterval = ENDED_INTERVAL;
  }

  const telemetry = usePolling<Telemetry[]>(telemetryPath, telemetryInterval);
  const recordings = usePolling<Recording[]>(recordingsPath, RECORDINGS_INTERVAL);

  let rows: Telemetry[] = [];

  if (telemetry.data !== null) {
    rows = sortByTime(telemetry.data);
  }

  const latest = rows.at(-1);
  let online = false;

  if (selected !== undefined && selected.status === "IN_PROGRESS" && latest !== undefined) {
    online = now - telemetry.changedAt < ONLINE_TIMEOUT;
  }

  let groups: AlarmGroup[] | null = null;
  const alarmIds: number[] = [];

  if (alarms.data !== null) {
    groups = groupAlarms(alarms.data);

    for (const group of groups) {
      alarmIds.push(group.incidentId);
    }
  }

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError("");

    try {
      await action();
    } catch (failure) {
      let message = "Operacja nie powiodła się";

      if (failure instanceof Error) {
        message = failure.message;
      }

      setError(message);
    }

    setBusy(false);
    sessions.refresh();
    alarms.refresh();
  }

  function endSession() {
    if (selected === undefined) {
      return;
    }

    if (window.confirm(`Czy na pewno chcesz zakończyć sesję ${incidentName(selected)}?`)) {
      run(() => api(`/incidents/${selected.id}/end`, "PUT"));
    }
  }

  function resolveAlarm(group: AlarmGroup) {
    const question = `Oznaczyć alarm „${alarmNames[group.alarmType]}” (${nameOf(incidents, group.incidentId)}) jako rozwiązany?`;

    if (!window.confirm(question)) {
      return;
    }

    run(async () => {
      for (const id of group.ids) {
        await api(`/alarms/${id}/resolve`, "PUT");
      }
    });
  }

  function showAlarm(group: AlarmGroup) {
    setSelectedId(group.incidentId);
  }

  let themeLabel = "Ciemny motyw";

  if (theme === "dark") {
    themeLabel = "Jasny motyw";
  }

  let sessionsError = null;

  if (sessions.error !== "") {
    sessionsError = <p className="orange">{errorText(sessions.error, sessions.updatedAt)}</p>;
  }

  let sessionView = (
    <p className="card muted empty wide">Wybierz sesję z listy, aby zobaczyć obraz z kamery i pomiary</p>
  );

  if (selected !== undefined) {
    let camera = <p className="card muted empty">Sesja zakończona, obraz na żywo jest niedostępny</p>;
    let recordingList = null;
    let telemetryError = null;

    if (selected.status === "IN_PROGRESS") {
      camera = <CameraPanel key={selected.id} incidentId={selected.id} />;
    }

    if (recordings.data !== null) {
      recordingList = <RecordingList recordings={recordings.data} />;
    }

    if (telemetry.error !== "") {
      telemetryError = <p className="orange">{errorText(telemetry.error, telemetry.updatedAt)}</p>;
    }

    sessionView = (
      <section className="content" aria-label="Kamera">
        {telemetryError}
        {camera}
        {recordingList}
      </section>
    );
  }

  let toast = null;

  if (error !== "") {
    toast = (
      <div className="toast glass" role="alert">
        <span className="orange">{error}</span>
        <button onClick={() => setError("")}>Zamknij</button>
      </div>
    );
  }

  return (
    <>
      <header className="topbar glass">
        <h1>INFRA VIEW</h1>
        <SessionBar
          incident={selected}
          latest={latest}
          online={online}
          busy={busy}
          onEnd={endSession}
          onClose={() => setSelectedId(null)}
        />
        <nav className="toolbar">
          <button onClick={toggleTheme}>{themeLabel}</button>
        </nav>
      </header>

      <main className="layout">
        <aside className="pane glass">
          {sessionsError}
          <SessionList incidents={active} selectedId={selectedId} alarmIds={alarmIds} onSelect={setSelectedId} />
          <AlarmList
            groups={groups}
            incidents={incidents}
            error={alarms.error}
            busy={busy}
            onSelect={showAlarm}
            onResolve={resolveAlarm}
          />
        </aside>

        {sessionView}
      </main>

      {toast}
    </>
  );
}
