import { useEffect, useRef, useState } from "react";
import type { Alarm, AlarmGroup, Incident, Telemetry, Threshold } from "./types";
import { api, errorMessage } from "./api";
import { findIncident, groupAlarms } from "./data";
import { alarmNames, ONLINE_TIMEOUT } from "./format";
import { usePolling } from "./usePolling";
import { SessionBar } from "./SessionBar";
import { SessionList } from "./SessionList";
import { AlarmList } from "./AlarmList";
import { AlarmPopups } from "./AlarmPopups";
import { SessionView } from "./SessionView";
import { ArchiveDialog } from "./ArchiveDialog";
import { ThresholdsDialog } from "./ThresholdsDialog";
import { ThemeToggle } from "./ThemeToggle";
import { Toast } from "./Toast";
import { Confirm } from "./Confirm";
import { ErrorLine } from "./ErrorLine";

const SESSIONS_INTERVAL = 5000;
const ALARMS_INTERVAL = 2000;
const THRESHOLDS_INTERVAL = 30000;
const LIVE_INTERVAL = 1000;
const ENDED_INTERVAL = 30000;

type Confirmation = {
  title: string;
  text: string;
  button: string;
  action: () => Promise<unknown>;
};

export default function App() {
  const sessions = usePolling<Incident[]>("/incidents", SESSIONS_INTERVAL);
  const alarms = usePolling<Alarm[]>("/alarms/active", ALARMS_INTERVAL);
  const thresholds = usePolling<Threshold[]>("/alarm-thresholds", THRESHOLDS_INTERVAL);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [dialog, setDialog] = useState("");
  const [dismissed, setDismissed] = useState<number[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [now, setNow] = useState(Date.now);
  const opening = useRef(0);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  let incidents: Incident[] = [];

  if (sessions.data !== null) {
    incidents = sessions.data;
  }

  const selected = findIncident(incidents, selectedId);
  let telemetryPath = null;
  let telemetryInterval = LIVE_INTERVAL;

  if (selected !== undefined) {
    telemetryPath = `/telemetry/incident/${selected.id}`;
  }

  if (selected !== undefined && selected.status !== "IN_PROGRESS") {
    telemetryInterval = ENDED_INTERVAL;
  }

  const telemetry = usePolling<Telemetry[]>(telemetryPath, telemetryInterval);
  let rows: Telemetry[] = [];

  if (telemetry.data !== null) {
    rows = telemetry.data;
  }

  const latest = rows.at(-1);
  let online = false;

  if (selected !== undefined && selected.status === "IN_PROGRESS" && latest !== undefined) {
    online = now - telemetry.changedAt < ONLINE_TIMEOUT;
  }

  let groups: AlarmGroup[] | null = null;
  const alarmIds: number[] = [];
  const seen: number[] = [];

  if (alarms.data !== null) {
    groups = groupAlarms(alarms.data, incidents);

    for (const group of groups) {
      alarmIds.push(group.incidentId);

      if (group.alarmType !== "SOS") {
        seen.push(group.latestId);
      }
    }
  }

  if (groups !== null && dismissed === null) {
    setDismissed(seen);
  }

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError("");

    try {
      await action();
    } catch (failure) {
      setError(errorMessage(failure, "Operacja nie powiodła się"));
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

    setConfirmation({
      title: "Zakończyć sesję?",
      text: `Sesja ${selected.code} zostanie zakończona.`,
      button: "Zakończ sesję",
      action: () => api(`/incidents/${selected.id}/end`, "PUT"),
    });
  }

  function resolveAlarm(group: AlarmGroup) {
    setConfirmation({
      title: "Rozwiązać alarm?",
      text: `Alarm „${alarmNames[group.alarmType]}” (${group.name}) zostanie oznaczony jako rozwiązany.`,
      button: "Rozwiąż",
      action: async () => {
        for (const id of group.ids) {
          await api(`/alarms/${id}/resolve`, "PUT");
        }
      },
    });
  }

  function accept() {
    if (confirmation !== null) {
      run(confirmation.action);
      setConfirmation(null);
    }
  }

  function openDialog(name: string) {
    opening.current = opening.current + 1;
    setDialog(name);
  }

  function saveAndClose(action: () => Promise<unknown>) {
    const opened = opening.current;

    run(async () => {
      await action();

      if (opening.current === opened) {
        setDialog("");
      }
    });
  }

  function saveThresholds(list: Threshold[]) {
    saveAndClose(async () => {
      for (const item of list) {
        await api(`/alarm-thresholds/${item.metric}`, "PUT", {
          threshold: item.threshold,
          resetMargin: item.resetMargin,
          enabled: item.enabled,
        });
      }
    });
  }

  function dismissAlarm(group: AlarmGroup) {
    if (dismissed !== null) {
      setDismissed([...dismissed, group.latestId]);
    }
  }

  function showAlarm(group: AlarmGroup) {
    setSelectedId(group.incidentId);
    dismissAlarm(group);
  }

  function closeDialog() {
    setDialog("");
  }

  function openSession(id: number) {
    setSelectedId(id);
    closeDialog();
  }

  let popups = null;

  if (groups !== null && dismissed !== null) {
    popups = (
      <AlarmPopups
        groups={groups}
        dismissed={dismissed}
        onShow={showAlarm}
        onDismiss={dismissAlarm}
      />
    );
  }

  let dialogWindow = null;

  if (dialog === "archive") {
    dialogWindow = <ArchiveDialog incidents={incidents} onOpen={openSession} onClose={closeDialog} />;
  }

  if (dialog === "thresholds") {
    dialogWindow = (
      <ThresholdsDialog
        thresholds={thresholds.data}
        error={thresholds.error}
        busy={busy}
        onSave={saveThresholds}
        onClose={closeDialog}
      />
    );
  }

  if (confirmation !== null) {
    dialogWindow = (
      <Confirm
        title={confirmation.title}
        text={confirmation.text}
        button={confirmation.button}
        onYes={accept}
        onNo={() => setConfirmation(null)}
      />
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
          <button onClick={() => openDialog("archive")}>Archiwum</button>
          <button onClick={() => openDialog("thresholds")}>Progi alarmów</button>
          <ThemeToggle />
        </nav>
      </header>

      <main className="layout">
        <aside className="pane glass">
          <ErrorLine error={sessions.error} updatedAt={sessions.updatedAt} />
          <SessionList incidents={sessions.data} selectedId={selectedId} alarmIds={alarmIds} onSelect={setSelectedId} />
          <AlarmList
            groups={groups}
            error={alarms.error}
            busy={busy}
            onSelect={showAlarm}
            onResolve={resolveAlarm}
          />
        </aside>

        <SessionView
          key={selectedId}
          incident={selected}
          rows={rows}
          thresholds={thresholds.data}
          error={telemetry.error}
          updatedAt={telemetry.updatedAt}
        />
      </main>

      {popups}
      {dialogWindow}
      <Toast message={error} onClose={() => setError("")} />
    </>
  );
}
