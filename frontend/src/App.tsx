import { useRef, useState } from "react";

import {
  createIncident,
  endIncident,
  getAlarms,
  getIncidents,
  resolveAlarm,
} from "./api";

import type { NewIncident } from "./types";

import { usePolling } from "./usePolling";
import { useTheme } from "./useTheme";

import { SessionList } from "./SessionList";
import { SessionDetails } from "./SessionDetails";
import { AlarmPanel } from "./AlarmPanel";
import { LoadStatus } from "./LoadStatus";
import { NewSessionForm } from "./NewSessionForm";

export default function App() {
  const { theme, toggleTheme } = useTheme();

  const sessions = usePolling(getIncidents, 5000);
  const alarms = usePolling(getAlarms, 1000);

  const [selectedId, setSelectedId] =
    useState<number | null>(null);

  const [showEnded, setShowEnded] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [actionError, setActionError] = useState("");

  const actionLock = useRef(false);

  const incidents = sessions.data ?? [];

  const visible = incidents.filter(
    incident =>
      showEnded || incident.status === "IN_PROGRESS"
  );

  const selected = visible.find(
    incident => incident.id === selectedId
  );

  async function perform(
    action: () => Promise<unknown>,
    successMessage: string
  ) {
    if (actionLock.current) {
      return false;
    }

    actionLock.current = true;
    setBusy(true);
    setMessage("");
    setActionError("");

    try {
      await action();
      setMessage(successMessage);

      return true;
    } catch (error) {
      setActionError(
        error instanceof Error
          ? error.message
          : "Błąd operacji"
      );

      return false;
    } finally {
      sessions.refresh();
      alarms.refresh();

      actionLock.current = false;
      setBusy(false);
    }
  }

  async function handleCreate(input: NewIncident) {
    return perform(async () => {
      const incident = await createIncident(input);

      sessions.update(previous => [
        ...(previous ?? []).filter(
          item => item.id !== incident.id
        ),
        incident,
      ]);

      setSelectedId(incident.id);
    }, "Sesja została utworzona.");
  }

  function handleEnd() {
    if (!selected) {
      return;
    }

    const confirmed = window.confirm(
      `Zakończyć sesję ${selected.code}?`
    );

    if (!confirmed) {
      return;
    }

    void perform(async () => {
      const ended = await endIncident(selected.id);

      sessions.update(previous =>
        (previous ?? []).map(incident =>
          incident.id === ended.id
            ? ended
            : incident
        )
      );
    }, "Sesja została zakończona.");
  }

  function handleResolve(id: number) {
    const confirmed = window.confirm(
      "Oznaczyć alarm jako rozwiązany?"
    );

    if (!confirmed) {
      return;
    }

    void perform(async () => {
      const resolved = await resolveAlarm(id);

      alarms.update(previous =>
        (previous ?? []).map(alarm =>
          alarm.id === resolved.id
            ? resolved
            : alarm
        )
      );
    }, "Alarm został rozwiązany.");
  }

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>infra-view</h1>
          <p className="muted">Panel monitoringu</p>
        </div>

        <button onClick={toggleTheme}>
          {theme === "dark"
            ? "Jasny motyw"
            : "Ciemny motyw"}
        </button>
      </header>

      {message && (
        <p role="status">{message}</p>
      )}

      {actionError && (
        <p className="error" role="alert">
          {actionError}
        </p>
      )}

      <LoadStatus
        label="Sesje"
        error={sessions.error}
        updatedAt={sessions.updatedAt}
      />

      <label>
        <span>
          <input
            type="checkbox"
            checked={showEnded}
            onChange={event =>
              setShowEnded(event.target.checked)
            }
          />
          {" "}Pokaż również zakończone
        </span>
      </label>

      <main className="layout">
        <div className="content">
          <SessionList
            incidents={visible}
            selectedId={selected?.id ?? null}
            onSelect={setSelectedId}
          />

          <NewSessionForm
            busy={busy}
            onCreate={handleCreate}
          />
        </div>

        <section className="content">
          <div className="panel row">
            <h2>
              {selected?.firefighterName ||
                selected?.code ||
                "Wybierz sesję"}
            </h2>

            {selected?.status === "IN_PROGRESS" && (
              <button
                disabled={busy || Boolean(sessions.error)}
                onClick={handleEnd}
              >
                Zakończ sesję
              </button>
            )}
          </div>

          {selected && (
            <SessionDetails
              key={selected.id}
              incident={selected}
            />
          )}

          <LoadStatus
            label="Alarmy"
            error={alarms.error}
            updatedAt={alarms.updatedAt}
          />

          {alarms.data !== null && (
            <AlarmPanel
              alarms={alarms.data}
              incidents={incidents}
              onResolve={handleResolve}
              busy={busy || Boolean(alarms.error)}
            />
          )}
        </section>
      </main>
    </div>
  );
}