import { useCallback, useEffect, useState } from "react";

import type { Incident } from "./types";

import { getRecordings, getTelemetry } from "./api";
import { prepareTelemetry } from "./telemetry";
import { usePolling } from "./usePolling";

import { LoadStatus } from "./LoadStatus";
import { Metrics } from "./Metrics";
import { Charts } from "./Charts";
import { CameraPanel } from "./CameraPanel";
import { RecordingList } from "./RecordingList";

export function SessionDetails({ incident }: { incident: Incident }) {
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
    <>
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

      <CameraPanel />

      <Charts rows={rows} />

      <LoadStatus
        label="Nagrania"
        error={recordings.error}
        updatedAt={recordings.updatedAt}
      />

      {recordings.data !== null && (
        <RecordingList recordings={recordings.data} />
      )}
    </>
  );
}