import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import type { Alarm } from "../src/types";
import { groupAlarms } from "../src/data";
import { RecordingList } from "../src/RecordingList";

function alarm(id: number, incidentId: number, resolvedAt: string | null): Alarm {
  return { id, incidentId, alarmType: "FALL", triggeredAt: "2026-09-21T10:00:00Z", resolvedAt };
}

it("łączy alarmy tego samego typu i strażaka, pomija rozwiązane", () => {
  const groups = groupAlarms([alarm(1, 1, null), alarm(2, 1, null), alarm(3, 2, null), alarm(4, 1, "2026-09-21T10:05:00Z")]);

  expect(groups).toHaveLength(2);
  expect(groups[0].incidentId).toBe(2);
  expect(groups[1].ids).toEqual([1, 2]);
});

it("trwające nagranie nie ma końca ani jednostek bez danych", () => {
  const recording = { id: 1, startedAt: "2026-09-21T10:00:00Z", endedAt: null, durationSec: null, fileSizeBytes: null };
  render(<RecordingList recordings={[recording]} />);

  expect(screen.getByText("trwa")).toBeTruthy();
  expect(screen.queryByText("— s")).toBeNull();
});
