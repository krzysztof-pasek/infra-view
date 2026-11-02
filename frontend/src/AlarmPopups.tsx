import type { AlarmGroup } from "./types";
import { AlarmInfo } from "./AlarmInfo";

type Props = {
  groups: AlarmGroup[];
  dismissed: number[];
  onShow: (group: AlarmGroup) => void;
  onDismiss: (group: AlarmGroup) => void;
};

export function AlarmPopups({ groups, dismissed, onShow, onDismiss }: Props) {
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
          <AlarmInfo group={group} />
          <div className="toolbar">
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
