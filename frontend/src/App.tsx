import { useState } from "react";

import { getAlarms, getIncidents } from "./api";
import { usePolling } from "./usePolling";
import { useTheme } from "./useTheme";

import { SessionList } from "./SessionList";
import { SessionDetails } from "./SessionDetails";
import { AlarmPanel } from "./AlarmPanel";
import { LoadStatus } from "./LoadStatus";

export default function App() {
  const { theme, toggleTheme } = useTheme();

  const sessions = usePolling(getIncidents, 5000);
  const alarms = usePolling(getAlarms, 1000);

  const [selectedId, setSelectedId] =
    useState<number | null>(null);

  const [showEnded, setShowEnded] = useState(false);

  const incidents = sessions.data ?? [];

  const visible = incidents.filter(
    incident =>
      showEnded || incident.status === "IN_PROGRESS"
  );

  const selected = visible.find(
    incident => incident.id === selectedId
  );

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>RescueVision</h1>
          <p className="muted">Panel monitoringu</p>
        </div>

        <button onClick={toggleTheme}>
          {theme === "dark"
            ? "Jasny motyw"
            : "Ciemny motyw"}
        </button>
      </header>

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
        <SessionList
          incidents={visible}
          selectedId={selected?.id ?? null}
          onSelect={setSelectedId}
        />

        <section className="content">
          <div className="panel">
            <h2>
              {selected?.firefighterName ||
                selected?.code ||
                "Wybierz sesję"}
            </h2>
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
            />
          )}
        </section>
      </main>
    </div>
  );
}