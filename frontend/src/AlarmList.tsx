import type { AlarmGroup } from "./types";
import { AlarmInfo } from "./AlarmInfo";
import { ErrorLine } from "./ErrorLine";

type Props = {
  groups: AlarmGroup[] | null;
  error: string;
  busy: boolean;
  onSelect: (group: AlarmGroup) => void;
  onResolve: (group: AlarmGroup) => void;
};

export function AlarmList({ groups, error, busy, onSelect, onResolve }: Props) {
  let content = null;

  if (groups === null && error === "") {
    content = <p className="muted">Ładowanie…</p>;
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

  return (
    <section aria-label="Aktywne alarmy">
      <h2>Aktywne alarmy</h2>
      <ErrorLine error={error} />
      {content}
    </section>
  );
}
