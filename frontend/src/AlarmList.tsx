import type { AlarmGroup, Incident } from "./types";
import { nameOf } from "./data";
import { alarmNames, formatTime } from "./format";

type Props = {
  groups: AlarmGroup[] | null;
  incidents: Incident[];
  error: string;
  busy: boolean;
  onSelect: (group: AlarmGroup) => void;
  onResolve: (group: AlarmGroup) => void;
};

export function AlarmList({ groups, incidents, error, busy, onSelect, onResolve }: Props) {
  let content = <p className="muted">Ładowanie…</p>;

  if (error !== "" && groups === null) {
    content = <p className="orange">{error}</p>;
  }

  if (groups !== null && groups.length === 0) {
    content = <p className="muted">Brak aktywnych alarmów</p>;
  }

  if (groups !== null && groups.length > 0) {
    content = (
      <ul className="list">
        {groups.map(group => (
          <li className="alarm" key={group.key}>
            <button className="alarm-info" onClick={() => onSelect(group)}>
              <strong>{alarmTitle(group)}</strong>
              <span>{nameOf(incidents, group.incidentId)}</span>
              <small>{formatTime(group.triggeredAt)}</small>
            </button>
            <button disabled={busy} onClick={() => onResolve(group)}>
              Rozwiąż
            </button>
          </li>
        ))}
      </ul>
    );
  }

  let warning = null;

  if (error !== "" && groups !== null) {
    warning = <p className="orange">{error}</p>;
  }

  return (
    <section aria-label="Aktywne alarmy">
      <h2 className="pane-title">Aktywne alarmy</h2>
      {warning}
      {content}
    </section>
  );
}

export function alarmTitle(group: AlarmGroup) {
  if (group.ids.length > 1) {
    return `${alarmNames[group.alarmType]} ×${group.ids.length}`;
  }

  return alarmNames[group.alarmType];
}
