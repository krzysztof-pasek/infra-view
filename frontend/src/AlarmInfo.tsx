import type { AlarmGroup } from "./types";
import { alarmTitle, formatTime } from "./format";

export function AlarmInfo({ group }: { group: AlarmGroup }) {
  return (
    <>
      <strong className="red">{alarmTitle(group)}</strong>
      <span>{group.name}</span>
      <small>{formatTime(group.triggeredAt)}</small>
    </>
  );
}
