import type { Alarm, Incident } from "./types";
import { formatDate } from "./format";

const labels: Record<Alarm["alarmType"], string> = {
  HIGH_TEMPERATURE: "Wysoka temperatura",
  NO_MOTION: "Brak ruchu",
  HIGH_GAS: "Wysokie stężenie gazu",
  FALL: "Upadek",
};

type Props = {
  alarms: Alarm[];
  incidents: Incident[];
  onResolve?: (id: number) => void;
  busy?: boolean;
};

export function AlarmPanel({
  alarms,
  incidents,
  onResolve,
  busy = false,
}: Props) {
  const sorted = [...alarms].sort(
    (a, b) => Date.parse(b.triggeredAt) - Date.parse(a.triggeredAt)
  );

  const active = sorted.filter(alarm => alarm.resolvedAt === null);
  const history = sorted.filter(alarm => alarm.resolvedAt !== null);

  function owner(alarm: Alarm) {
    const incident = incidents.find(
      incident => incident.id === alarm.incidentId
    );

    return (
      incident?.firefighterName ||
      incident?.code ||
      `Sesja ${alarm.incidentId}`
    );
  }

  return (
    <section className="panel">
      <h2>Aktywne alarmy ({active.length})</h2>

      {active.length === 0 && (
        <p className="muted">
          Brak aktywnych alarmów w pobranych danych
        </p>
      )}

      <ul className="alarm-list">
        {active.map(alarm => (
          <li className="alarm" key={alarm.id}>
            <div className="row">
              <strong>
                {labels[alarm.alarmType]}: {owner(alarm)}
              </strong>

              {onResolve && (
                <button
                  disabled={busy}
                  onClick={() => onResolve(alarm.id)}
                >
                  Rozwiąż alarm
                </button>
              )}
            </div>

            <small>{formatDate(alarm.triggeredAt)}</small>
          </li>
        ))}
      </ul>

      <details>
        <summary>
          Historia rozwiązanych alarmów ({history.length})
        </summary>

        {history.map(alarm => (
          <p key={alarm.id}>
            {labels[alarm.alarmType]}: {owner(alarm)}. Rozwiązano:{" "}
            {formatDate(alarm.resolvedAt)}
          </p>
        ))}
      </details>
    </section>
  );
}