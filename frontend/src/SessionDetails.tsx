import { useEffect, useState } from "react";

import type { Incident, Telemetry } from "./types";
import { Metrics } from "./Metrics";
import { Charts } from "./Charts";

export function SessionDetails({ incident }: { incident: Incident }) {
  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const rows: Telemetry[] = [];

  return (
    <>
      <Metrics
        latest={rows.at(-1)}
        now={now}
        ended={incident.status === "RESOLVED"}
      />

      <Charts rows={rows} />
    </>
  );
}