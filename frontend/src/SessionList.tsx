import type { Incident } from "./types";
import { hasName, incidentName } from "./data";
import { formatTime } from "./format";

type Props = {
  incidents: Incident[] | null;
  selectedId: number | null;
  alarmIds: number[];
  onSelect: (id: number) => void;
};

export function SessionList({ incidents, selectedId, alarmIds, onSelect }: Props) {
  let count = 0;
  let content = <p className="muted">Ładowanie…</p>;

  if (incidents !== null) {
    count = incidents.length;
    content = <p className="muted">Brak aktywnych sesji</p>;
  }

  if (incidents !== null && incidents.length > 0) {
    content = (
      <ul className="list">
        {incidents.map(incident => (
          <li key={incident.id}>
            <SessionItem
              incident={incident}
              selected={incident.id === selectedId}
              alarm={alarmIds.includes(incident.id)}
              onSelect={onSelect}
            />
          </li>
        ))}
      </ul>
    );
  }

  return (
    <details open>
      <summary>Sesje ({count})</summary>
      {content}
    </details>
  );
}

type ItemProps = {
  incident: Incident;
  selected: boolean;
  alarm: boolean;
  onSelect: (id: number) => void;
};

function SessionItem({ incident, selected, alarm, onSelect }: ItemProps) {
  let dotColor = "muted";
  let detail = incident.code;

  if (alarm) {
    dotColor = "red";
  }

  if (!hasName(incident)) {
    detail = "start " + formatTime(incident.startedAt);
  }

  return (
    <button className="session" aria-pressed={selected} onClick={() => onSelect(incident.id)}>
      <span className={"dot " + dotColor} />
      <span>
        <strong>{incidentName(incident)}</strong>
        <small>{detail}</small>
      </span>
    </button>
  );
}
