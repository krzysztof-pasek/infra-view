import { useEffect, useState } from "react";
import type { Alarm, AlarmGroup, Incident, Metric, Recording, Telemetry, Threshold } from "./types";
import { api } from "./api";
import { findIncident, groupAlarms, incidentName, nameOf, sortByTime } from "./data";
import { alarmNames, formatTime, ONLINE_TIMEOUT } from "./format";
import { usePolling } from "./usePolling";
import { useTheme } from "./useTheme";
import { SessionBar } from "./SessionBar";
import { SessionList } from "./SessionList";
import { AlarmList } from "./AlarmList";
import { AlarmPopups } from "./AlarmPopups";
import { CameraPanel } from "./CameraPanel";
import { Charts } from "./Charts";
import { RecordingList } from "./RecordingList";
import { ArchiveDialog } from "./ArchiveDialog";
import { ThresholdsDialog } from "./ThresholdsDialog";
import type { ThresholdValues } from "./ThresholdsDialog";

const SESSIONS_INTERVAL = 5000;
const ALARMS_INTERVAL = 2000;
const THRESHOLDS_INTERVAL = 30000;
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
  const thresholds = usePolling<Threshold[]>("/alarm-thresholds", THRESHOLDS_INTERVAL);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [dialog, setDialog] = useState("");
  const [dismissed, setDismissed] = useState<number[] | null>(null);
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

  if (groups !== null && dismissed === null) {
    const seen: number[] = [];

    for (const group of groups) {
      seen.push(group.latestId);
    }

    setDismissed(seen);
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
    thresholds.refresh();
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

  function saveThreshold(metric: Metric, values: ThresholdValues) {
    run(() => api(`/alarm-thresholds/${metric}`, "PUT", values));
  }

  function dismissAlarm(group: AlarmGroup) {
    let list: number[] = [];

    if (dismissed !== null) {
      list = dismissed;
    }

    setDismissed([...list, group.latestId]);
  }

  function showAlarm(group: AlarmGroup) {
    setSelectedId(group.incidentId);
    dismissAlarm(group);
  }

  function openSession(id: number) {
    setSelectedId(id);
    setDialog("");
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
      <>
        <section className="content" aria-label="Kamera">
          {camera}
          {recordingList}
        </section>

        <section className="content charts-column" aria-label="Wykresy">
          {telemetryError}
          <Charts key={selected.id} rows={rows} thresholds={thresholds.data} />
        </section>
      </>
    );
  }

  let popups = null;

  if (groups !== null && dismissed !== null) {
    popups = (
      <AlarmPopups
        groups={groups}
        dismissed={dismissed}
        incidents={incidents}
        onShow={showAlarm}
        onDismiss={dismissAlarm}
      />
    );
  }

  let dialogWindow = null;

  if (dialog === "archive") {
    dialogWindow = <ArchiveDialog incidents={incidents} onOpen={openSession} onClose={() => setDialog("")} />;
  }

  if (dialog === "thresholds") {
    dialogWindow = (
      <ThresholdsDialog
        thresholds={thresholds.data}
        error={thresholds.error}
        busy={busy}
        onSave={saveThreshold}
        onClose={() => setDialog("")}
      />
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
          <button onClick={() => setDialog("archive")}>Archiwum</button>
          <button onClick={() => setDialog("thresholds")}>Progi alarmów</button>
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

      {popups}
      {dialogWindow}
      {toast}
    </>
  );
}
