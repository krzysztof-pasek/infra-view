import type { Incident, Telemetry } from "./types";
import { incidentName } from "./data";
import { formatDate, motionStates } from "./format";

type Props = {
  incident: Incident | undefined;
  latest: Telemetry | undefined;
  online: boolean;
  busy: boolean;
  onEnd: () => void;
  onClose: () => void;
};

export function SessionBar({ incident, latest, online, busy, onEnd, onClose }: Props) {
  if (incident === undefined) {
    return <div className="session-bar" />;
  }

  let status = "Zakończona";
  let statusColor = "muted";
  let action = <button onClick={onClose}>Zamknij</button>;

  if (incident.status === "IN_PROGRESS") {
    status = "Brak danych z kasku";
    statusColor = "orange";
    action = (
      <button disabled={busy} onClick={onEnd}>
        Zakończ sesję
      </button>
    );
  }

  if (incident.status === "IN_PROGRESS" && online) {
    status = "Na żywo";
    statusColor = "";
  }

  let detail = incident.code + " · start " + formatDate(incident.startedAt);

  if (incidentName(incident) === incident.code) {
    detail = "start " + formatDate(incident.startedAt);
  }

  let motion = "—";
  let motionColor = "";

  if (latest !== undefined && latest.motionState !== null) {
    motion = motionStates[latest.motionState].label;
    motionColor = motionStates[latest.motionState].color;
  }

  return (
    <div className="session-bar">
      <div>
        <h2>{incidentName(incident)}</h2>
        <small>{detail}</small>
      </div>

      <span className={"status " + statusColor}>
        <span className="dot" />
        {status}
      </span>

      <span className={"status " + motionColor}>Ruch: {motion}</span>

      {action}
    </div>
  );
}
