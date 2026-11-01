import type { AlarmGroup } from "./types";
import { AlarmInfo } from "./AlarmInfo";

type Props = {
  groups: AlarmGroup[] | null;
  error: string;
  busy: boolean;
  onSelect: (group: AlarmGroup) => void;
  onResolve: (group: AlarmGroup) => void;
};

export function AlarmList({ groups, error, busy, onSelect, onResolve }: Props) {
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
              <AlarmInfo group={group} />
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
      <h2>Aktywne alarmy</h2>
      {warning}
      {content}
    </section>
  );
}
