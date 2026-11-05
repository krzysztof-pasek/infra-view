import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { Alarm, AlarmType, Incident } from "../src/types";
import { groupAlarms } from "../src/data";
import { alarmTitle, formatAge, formatDelay } from "../src/format";
import { SessionBar } from "../src/SessionBar";
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
});

it("pokazuje, jak dawno przyszedł ostatni pomiar", () => {
  expect(formatAge(25400)).toBe("25 s");
  expect(formatAge(-3000)).toBe("0 s");
  expect(formatAge(185000)).toBe("3 min");
  expect(formatAge(7300000)).toBe("2 h");

  const latest = { recordedAt: session.startedAt, temperature: null, co2Ppm: null, tvocPpb: null, gasVoltage: null, motionState: null };
  const now = Date.parse(session.startedAt) + 25000;
  const handlers = { onEnd: () => {}, onRename: () => {}, onClose: () => {} };
  const view = render(<SessionBar incident={session} latest={latest} online={false} now={now} busy={false} {...handlers} />);

  expect(screen.getByText("Brak danych z kasku · 25 s")).toBeTruthy();
  view.rerender(<SessionBar incident={session} latest={latest} online={true} now={now} busy={false} {...handlers} />);
  expect(screen.getByText("Na żywo")).toBeTruthy();
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
