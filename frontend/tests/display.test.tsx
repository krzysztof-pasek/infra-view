import { expect, it } from "vitest";
import type { Alarm, AlarmType } from "../src/types";
import { groupAlarms } from "../src/data";
import { alarmTitle, formatDelay } from "../src/format";

function alarm(id: number, incidentId: number, alarmType: AlarmType = "FALL"): Alarm {
  return { id, incidentId, alarmType, triggeredAt: "2026-09-21T10:00:00Z", resolvedAt: null };
}

it("łączy alarmy tego samego typu i strażaka", () => {
  const groups = groupAlarms([alarm(1, 1), alarm(2, 1), alarm(3, 2), alarm(4, 1, "SOS")]);

  expect(groups).toHaveLength(3);
  expect(groups[0].alarmType).toBe("SOS");
  expect(groups[1].incidentId).toBe(2);
  expect(groups[2].ids).toEqual([1, 2]);
  expect(alarmTitle(groups[2])).toBe("Upadek ×2");
});

it("alarm SOS ma czytelną nazwę", () => {
  const groups = groupAlarms([alarm(1, 1, "SOS")]);

  expect(alarmTitle(groups[0])).toBe("SOS");
});

it("pokazuje opóźnienie powtórki w minutach", () => {
  expect(formatDelay(150)).toBe("−2:30");
  expect(formatDelay(5)).toBe("−0:05");
});
