import { useState } from "react";
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
  const [shownKeys, setShownKeys] = useState<string[]>([]);
  const keys: string[] = [];

  for (const group of groups) {
    keys.push(group.key);
  }

  for (const key of shownKeys) {
    if (!keys.includes(key)) {
      keys.push(key);
    }
  }

  if (keys.length > shownKeys.length) {
    setShownKeys(keys);
  }

  function popup(key: string) {
    for (const group of groups) {
      if (group.key === key && !dismissed.includes(group.latestId)) {
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
    }

    return null;
  }

  return (
    <div className="popups">
      {keys.map(key => (
        <Appear key={key}>{popup(key)}</Appear>
      ))}
    </div>
  );
}
