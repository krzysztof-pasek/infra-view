import { useCallback, useEffect, useState } from "react";

import type { ReactNode } from "react";

import type { Incident } from "./types";

import { getRecordings, getTelemetry } from "./api";
import { prepareTelemetry } from "./telemetry";
import { usePolling } from "./usePolling";

import { LoadStatus } from "./LoadStatus";
import { Metrics } from "./Metrics";
import { Charts } from "./Charts";
import { CameraPanel } from "./CameraPanel";
import { RecordingList } from "./RecordingList";

type Props = {
  incident: Incident | undefined;
  busy: boolean;
  onEnd: () => void;
  alarms: ReactNode;
};

export function SessionDetails({ incident, busy, onEnd, alarms }: Props) {
  if (!incident) {
    return (
      <div className="session-layout">
        <section className="panel"><h2>Wybierz sesję</h2></section>
        <aside className="content" aria-label="Alarmy wszystkich sesji">
          {alarms}
        </aside>
      </div>
    );
  }

  return (
    <SelectedSession
      key={incident.id}
      incident={incident}
      busy={busy}
      onEnd={onEnd}
      alarms={alarms}
    />
  );
}

function SelectedSession({ incident, busy, onEnd, alarms }: Props & { incident: Incident }) {
  const loadTelemetry = useCallback(
    (signal: AbortSignal) =>
      getTelemetry(incident.id, signal),
    [incident.id]
  );

  const loadRecordings = useCallback(
    (signal: AbortSignal) =>
      getRecordings(incident.id, signal),
    [incident.id]
  );

  const telemetry = usePolling(
    loadTelemetry,
    incident.status === "IN_PROGRESS" ? 1000 : 30000
  );

  const recordings = usePolling(loadRecordings, 10000);

  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const rows = prepareTelemetry(
    telemetry.data ?? [],
    incident.id
  );

  return (
    <div className="session-layout">
      <section className="content" aria-label="Wybrana sesja">
        <div className="panel row">
          <h2>{incident.firefighterName || incident.code}</h2>
          {incident.status === "IN_PROGRESS" && (
            <button disabled={busy} onClick={onEnd}>Zakończ sesję</button>
          )}
        </div>

        <CameraPanel />

        <div className="content">
          <LoadStatus
            label="Nagrania"
            error={recordings.error}
            updatedAt={recordings.updatedAt}
          />
          {recordings.data !== null && (
            <RecordingList recordings={recordings.data} />
          )}
        </div>

        <section className="content" aria-label="Historia pomiarów">
          <p className="muted">Wykresy: ostatnie 120 pomiarów wybranej sesji.</p>
          <Charts rows={rows} />
        </section>
      </section>

      <aside className="content monitoring" aria-label="Pomiary i alarmy">
        <LoadStatus
          label="Pomiary"
          error={telemetry.error}
          updatedAt={telemetry.updatedAt}
        />
        <Metrics
          latest={rows.at(-1)}
          now={now}
          ended={incident.status === "RESOLVED"}
        />
        {alarms}
      </aside>
    </div>
  );
}