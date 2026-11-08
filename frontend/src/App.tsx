import { useEffect, useRef, useState } from "react";
import type { Alarm, AlarmGroup, Incident, Telemetry, Threshold } from "./types";
import { api, errorMessage } from "./api";
import { findIncident, groupAlarms, lastItem } from "./data";
import { alarmNames, ONLINE_TIMEOUT } from "./config";
import { usePolling } from "./usePolling";
import { SessionBar } from "./SessionBar";
import { SessionList } from "./SessionList";
import { AlarmList } from "./AlarmList";
import { AlarmPopups } from "./AlarmPopups";
import { SessionView } from "./SessionView";
import { ArchiveDialog } from "./ArchiveDialog";
import { ThresholdsDialog } from "./ThresholdsDialog";
import { RenameDialog } from "./RenameDialog";
import { ThemeToggle } from "./ThemeToggle";
import { Toast } from "./Toast";
import { Confirm } from "./Confirm";
import { ErrorLine } from "./ErrorLine";
import { Appear } from "./Appear";

const SESSIONS_INTERVAL = 5000;
const ALARMS_INTERVAL = 2000;
const THRESHOLDS_INTERVAL = 30000;
const LIVE_INTERVAL = 1000;
const ENDED_INTERVAL = 30000;
const HISTORY_LIMIT = 3600;

type Confirmation = {
  title: string;
  text: string;
  button: string;
  action: () => Promise<unknown>;
};

export function App() {
  const sessions = usePolling<Incident[]>("/incidents", SESSIONS_INTERVAL);
  const alarms = usePolling<Alarm[]>("/alarms/active", ALARMS_INTERVAL);
  const thresholds = usePolling<Threshold[]>("/alarm-thresholds", THRESHOLDS_INTERVAL);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [renameId, setRenameId] = useState<number | null>(null);
  const [dialog, setDialog] = useState("");
  const [dismissed, setDismissed] = useState<number[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [now, setNow] = useState(Date.now);
  const openCount = useRef(0);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  let incidents: Incident[] = [];

  if (sessions.data !== null) {
    incidents = sessions.data;
  }

  const selectedIncident = findIncident(incidents, selectedId);
  const renamedIncident = findIncident(incidents, renameId);
  let telemetryPath = null;
  let telemetryInterval = LIVE_INTERVAL;
  let historyPath = null;

  if (selectedIncident !== null) {
    telemetryPath = `/telemetry/incident/${selectedIncident.id}`;
  }

  if (selectedIncident !== null && selectedIncident.status !== "IN_PROGRESS") {
    telemetryInterval = ENDED_INTERVAL;
    historyPath = `${telemetryPath}?limit=${HISTORY_LIMIT}`;
  }

  const telemetry = usePolling<Telemetry[]>(telemetryPath, telemetryInterval);
  const history = usePolling<Telemetry[]>(historyPath, ENDED_INTERVAL);
  let rows: Telemetry[] = [];

  if (telemetry.data !== null) {
    rows = telemetry.data;
  }

  if (history.data !== null) {
    rows = history.data;
  }

  const latest = lastItem(rows);
  let online = false;

  if (selectedIncident !== null && selectedIncident.status === "IN_PROGRESS" && latest !== null) {
    online = now - telemetry.changedAt < ONLINE_TIMEOUT;
  }

  let groups: AlarmGroup[] | null = null;
  const alarmIncidentIds: number[] = [];
  const seen: number[] = [];

  if (alarms.data !== null) {
    groups = groupAlarms(alarms.data, incidents);

    for (const group of groups) {
      alarmIncidentIds.push(group.incidentId);

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
    if (selectedIncident === null) {
      return;
    }

    setConfirmation({
      title: "Zakończyć sesję?",
      text: `Sesja ${selectedIncident.code} zostanie zakończona.`,
      button: "Zakończ sesję",
      action: () => api(`/incidents/${selectedIncident.id}/end`, "PUT"),
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
    openCount.current = openCount.current + 1;
    setDialog(name);
  }

  function closeDialog() {
    setDialog("");
  }

  function saveAndClose(action: () => Promise<unknown>) {
    const countBefore = openCount.current;

    run(async () => {
      await action();

      if (openCount.current === countBefore) {
        closeDialog();
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

  function openRename() {
    setRenameId(selectedId);
    openDialog("rename");
  }

  function saveName(code: string) {
    if (renamedIncident !== null) {
      saveAndClose(() => api(`/incidents/${renamedIncident.id}/code`, "PUT", { code }));
    }
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

  let toast = null;

  if (error !== "") {
    toast = <Toast message={error} onClose={() => setError("")} />;
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

  if (dialog === "rename" && renamedIncident !== null) {
    dialogWindow = <RenameDialog code={renamedIncident.code} busy={busy} onSave={saveName} onClose={closeDialog} />;
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
      <div inert={dialogWindow !== null}>
        <header className="topbar glass">
          <h1>INFRA VIEW</h1>
          <SessionBar
            incident={selectedIncident}
            latest={latest}
            online={online}
            now={now}
            busy={busy}
            onEnd={endSession}
            onRename={openRename}
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
            <SessionList
              incidents={sessions.data}
              error={sessions.error}
              selectedId={selectedId}
              alarmIncidentIds={alarmIncidentIds}
              onSelect={setSelectedId}
            />
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
            incident={selectedIncident}
            rows={rows}
            thresholds={thresholds.data}
            error={telemetry.error}
            updatedAt={telemetry.updatedAt}
            now={now}
            online={online}
          />
        </main>

        {popups}
      </div>

      <Appear>{dialogWindow}</Appear>
      <Appear>{toast}</Appear>
    </>
  );
}
