import { fireEvent, render } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { Alarm, AlarmType, Incident } from "../src/types";
import { groupAlarms } from "../src/data";
import { alarmTitle, formatDelay } from "../src/format";
import { Modal } from "../src/Modal";

const session: Incident = { id: 1, code: "Jan Kowalski", startedAt: "2026-09-21T10:00:00Z", endedAt: null, status: "IN_PROGRESS" };

function alarm(id: number, incidentId: number, alarmType: AlarmType = "FALL"): Alarm {
  return { id, incidentId, alarmType, triggeredAt: "2026-09-21T10:00:00Z" };
}

it("łączy alarmy tego samego typu i strażaka", () => {
  const groups = groupAlarms([alarm(1, 1), alarm(2, 1), alarm(3, 2), alarm(4, 1, "SOS")], [session]);

  expect(groups).toHaveLength(3);
  expect(groups[0].alarmType).toBe("SOS");
  expect(groups[1].incidentId).toBe(2);
  expect(groups[2].ids).toEqual([1, 2]);
  expect(alarmTitle(groups[2])).toBe("Upadek ×2");
  expect(groups[2].name).toBe("Jan Kowalski");
  expect(groups[1].name).toBe("Sesja 2");
});

it("alarm SOS ma czytelną nazwę", () => {
  const groups = groupAlarms([alarm(1, 1, "SOS")], []);

  expect(alarmTitle(groups[0])).toBe("SOS");
});

it("pokazuje opóźnienie powtórki w minutach", () => {
  expect(formatDelay(150)).toBe("−2:30");
  expect(formatDelay(5)).toBe("−0:05");
  expect(formatDelay(0)).toBe("−0:00");
});

it("Esc zamyka tylko okno na wierzchu", () => {
  const lower = vi.fn();
  const upper = vi.fn();
  render(
    <>
      <Modal title="Dolne" onClose={lower}>
        <p>Dolne</p>
      </Modal>
      <Modal title="Górne" alert onClose={upper}>
        <p>Górne</p>
      </Modal>
    </>
  );

  fireEvent.keyDown(document.body, { key: "Escape" });

  expect(upper).toHaveBeenCalledTimes(1);
  expect(lower).not.toHaveBeenCalled();
});
