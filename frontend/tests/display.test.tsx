import { act, fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { Alarm, AlarmType } from "../src/types";
import { groupAlarms } from "../src/data";
import { RETRY_TIMEOUT } from "../src/config";
import { alarmTitle, formatAge, formatDelay } from "../src/format";
import { SessionBar } from "../src/SessionBar";
import { CameraPanel } from "../src/CameraPanel";
import { Modal } from "../src/Modal";
import { cameraImage, jan, row, START } from "./fixtures";

function alarm(id: number, incidentId: number, alarmType: AlarmType = "FALL"): Alarm {
  return { id, incidentId, alarmType, triggeredAt: jan.startedAt };
}

function sessionBar(online: boolean) {
  const handlers = { onEnd: () => {}, onRename: () => {}, onClose: () => {} };
  return <SessionBar incident={jan} latest={row(0, null)} online={online} now={START + 25000} busy={false} {...handlers} />;
}

function cameraPanel(path: string, live: boolean) {
  const handlers = { onSeek: () => {}, onPause: () => {}, onPlay: () => {} };
  return <CameraPanel path={path} live={live} paused={false} max={10} delay={0} label={null} {...handlers} />;
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

it("odświeżenie okna nie odpina nasłuchu Esc, a Esc woła aktualne zamknięcie", () => {
  const removeListener = vi.spyOn(document, "removeEventListener");
  const first = vi.fn();
  const second = vi.fn();
  const view = render(modal("Okno", first));

  view.rerender(modal("Okno", second));
  fireEvent.keyDown(document.body, { key: "Escape" });

  expect(removeListener).not.toHaveBeenCalledWith("keydown", expect.anything());
  expect(first).not.toHaveBeenCalled();
  expect(second).toHaveBeenCalledTimes(1);
});

it("po błędzie obrazu kamera na żywo łączy się ponownie, a nagranie czeka na przewinięcie", () => {
  vi.useFakeTimers();
  const view = render(cameraPanel("/api/incidents/1/live", true));

  fireEvent.error(cameraImage());
  expect(screen.getByText("Brak obrazu z kamery")).toBeTruthy();
  act(() => vi.advanceTimersByTime(RETRY_TIMEOUT));
  expect(cameraImage()).toBeTruthy();

  view.rerender(cameraPanel("/api/incidents/1/recording", false));
  fireEvent.error(cameraImage());
  act(() => vi.advanceTimersByTime(RETRY_TIMEOUT));
  expect(screen.getByText("Brak obrazu z kamery")).toBeTruthy();
});
