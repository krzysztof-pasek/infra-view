import type { Incident } from "./types";
import { formatTime } from "./format";
import { Loading } from "./Loading";

type Props = {
  incidents: Incident[] | null;
  selectedId: number | null;
  alarmIds: number[];
  onSelect: (id: number) => void;
};

export function SessionList({ incidents, selectedId, alarmIds, onSelect }: Props) {
  let count = 0;
  let content = <Loading />;

  if (incidents !== null) {
    const active: Incident[] = [];

    for (const incident of incidents) {
      if (incident.status === "IN_PROGRESS") {
        active.push(incident);
      }
    }

    count = active.length;
    content = <p className="muted">Brak aktywnych sesji</p>;

    if (active.length > 0) {
      content = (
        <ul className="list">
          {active.map(incident => (
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

  if (alarm) {
    dotColor = "red";
  }

  return (
    <button className="session" aria-pressed={selected} onClick={() => onSelect(incident.id)}>
      <span className={"dot " + dotColor} />
      <span>
        <strong>{incident.code}</strong>
        <small>start {formatTime(incident.startedAt)}</small>
      </span>
    </button>
  );
}
