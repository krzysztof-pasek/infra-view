import { useEffect, useState } from "react";

import type { Incident, Recording, Telemetry } from "./types";
import { Metrics } from "./Metrics";
import { Charts } from "./Charts";
import { CameraPanel } from "./CameraPanel";
import { RecordingList } from "./RecordingList";

export function SessionDetails({ incident }: { incident: Incident }) {
  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const rows: Telemetry[] = [];
  const recordings: Recording[] = [];

  return (
    <>
      <Metrics
        latest={rows.at(-1)}
        now={now}
        ended={incident.status === "RESOLVED"}
      />

      <CameraPanel />

      <Charts rows={rows} />

      <RecordingList recordings={recordings} />
    </>
  );
}