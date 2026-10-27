import type { AlarmGroup, Incident } from "./types";
import { nameOf } from "./data";
import { alarmTitle, formatTime } from "./format";

type Props = {
  groups: AlarmGroup[];
  dismissed: number[];
  incidents: Incident[];
  onShow: (group: AlarmGroup) => void;
  onDismiss: (group: AlarmGroup) => void;
};

export function AlarmPopups({ groups, dismissed, incidents, onShow, onDismiss }: Props) {
  const visible: AlarmGroup[] = [];

  for (const group of groups) {
    if (!dismissed.includes(group.latestId)) {
      visible.push(group);
    }
  }

  if (visible.length === 0) {
    return null;
  }

  return (
    <div className="popups">
      {visible.map(group => (
        <div className="popup glass" role="alert" key={group.key}>
          <strong>{alarmTitle(group)}</strong>
          <span>
            {nameOf(incidents, group.incidentId)} · {formatTime(group.triggeredAt)}
          </span>
          <div>
            <button className="primary" onClick={() => onShow(group)}>
              Pokaż
            </button>
            <button onClick={() => onDismiss(group)}>Zamknij</button>
          </div>
        </div>
      ))}
    </div>
  );
}
