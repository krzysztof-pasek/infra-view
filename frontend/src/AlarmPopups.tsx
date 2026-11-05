import type { AlarmGroup } from "./types";
import { Appear } from "./Appear";
import { AlarmInfo } from "./AlarmInfo";

type Props = {
  groups: AlarmGroup[];
  dismissed: number[];
  onShow: (group: AlarmGroup) => void;
  onDismiss: (group: AlarmGroup) => void;
};

export function AlarmPopups({ groups, dismissed, onShow, onDismiss }: Props) {
  function popup(group: AlarmGroup) {
    if (dismissed.includes(group.latestId)) {
      return null;
    }

    return (
      <div className="popup glass" role="alert">
        <AlarmInfo group={group} />
        <div className="toolbar">
          <button className="primary" onClick={() => onShow(group)}>
            Pokaż
          </button>
          <button onClick={() => onDismiss(group)}>Ukryj</button>
        </div>
      </div>
    );
  }

  return (
    <div className="popups">
      {groups.map(group => (
        <Appear key={group.key}>{popup(group)}</Appear>
      ))}
    </div>
  );
}
