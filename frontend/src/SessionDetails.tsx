import { useEffect, useState } from "react";

import type { Incident } from "./types";
import { Metrics } from "./Metrics";

export function SessionDetails({ incident }: { incident: Incident }) {
  const [now, setNow] = useState(Date.now);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <Metrics
      latest={undefined}
      now={now}
      ended={incident.status === "RESOLVED"}
    />
  );
}