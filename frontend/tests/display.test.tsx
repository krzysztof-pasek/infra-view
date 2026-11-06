import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { Alarm, AlarmType } from "../src/types";
import { groupAlarms } from "../src/data";
import { alarmTitle, formatAge, formatDelay } from "../src/format";
import { SessionBar } from "../src/SessionBar";
import { Modal } from "../src/Modal";
import { jan, row, START } from "./fixtures";

function alarm(id: number, incidentId: number, alarmType: AlarmType = "FALL"): Alarm {
  return { id, incidentId, alarmType, triggeredAt: jan.startedAt };
}

function sessionBar(online: boolean) {
  const handlers = { onEnd: () => {}, onRename: () => {}, onClose: () => {} };
  return <SessionBar incident={jan} latest={row(0, null)} online={online} now={START + 25000} busy={false} {...handlers} />;
}

function modal(title: string, onClose: () => void) {
  return (
    <Modal title={title} onClose={onClose}>
      <p>{title}</p>
    </Modal>
  );
}

it("łączy alarmy tego samego typu i strażaka", () => {
  const groups = groupAlarms([alarm(1, 1), alarm(2, 1), alarm(3, 2), alarm(4, 1, "SOS")], [jan]);

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

  const view = render(sessionBar(false));

  expect(screen.getByText("Brak danych z kasku · 25 s")).toBeTruthy();
  view.rerender(sessionBar(true));
  expect(screen.getByText("Na żywo")).toBeTruthy();
});

it("Esc zamyka tylko okno na wierzchu", () => {
  const lower = vi.fn();
  const upper = vi.fn();
  render(
    <>
      {modal("Dolne", lower)}
      {modal("Górne", upper)}
    </>
  );

  fireEvent.keyDown(document.body, { key: "Escape" });

  expect(upper).toHaveBeenCalledTimes(1);
  expect(lower).not.toHaveBeenCalled();
});
