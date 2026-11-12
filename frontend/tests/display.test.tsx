import { act, fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { Alarm, AlarmType, Incident } from "../src/types";
import { groupAlarms } from "../src/data";
import { RETRY_TIMEOUT } from "../src/config";
import { alarmTitle, formatAge, formatDelay } from "../src/format";
import { SessionBar } from "../src/SessionBar";
import { CameraPanel } from "../src/CameraPanel";
import { AlarmPopups } from "../src/AlarmPopups";
import { ChartTooltip } from "../src/Charts";
import { Modal } from "../src/Modal";
import { cameraImage, jan, row, START } from "./fixtures";

function alarm(id: number, incidentId: number, alarmType: AlarmType = "FALL"): Alarm {
  return { id, incidentId, alarmType, triggeredAt: jan.startedAt };
}

function sessionBar(incident: Incident, online: boolean, connecting: boolean) {
  const handlers = { onEnd: () => {}, onRename: () => {}, onClose: () => {} };
  const state = { online, connecting, now: START + 25000, busy: false };
  return <SessionBar incident={incident} latest={row(0, null)} {...state} {...handlers} />;
}

function cameraPanel(path: string, live: boolean) {
  const handlers = { onSeek: () => {}, onPause: () => {}, onPlay: () => {} };
  return <CameraPanel path={path} message="Brak obrazu z kamery" live={live} paused={false} max={10} delay={0} label={null} {...handlers} />;
}

function popups(alarms: Alarm[]) {
  return <AlarmPopups groups={groupAlarms(alarms, [jan])} dismissed={[]} onShow={() => {}} onDismiss={() => {}} />;
}

function popupTitles() {
  const titles = [];

  for (const popup of screen.getAllByRole("alert")) {
    titles.push(popup.children[0].textContent);
  }

  return titles;
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

it("alarmy SOS i gazu mają czytelne nazwy", () => {
  const groups = groupAlarms([alarm(1, 1, "SOS"), alarm(2, 1, "HIGH_GAS")], []);

  expect(alarmTitle(groups[1])).toBe("SOS");
  expect(alarmTitle(groups[0])).toBe("Wysoki poziom gazów");
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

  const view = render(sessionBar(jan, false, false));

  expect(screen.getByText("Brak danych z kasku · 25 s")).toBeTruthy();
  view.rerender(sessionBar(jan, true, false));
  expect(screen.getByText("Na żywo")).toBeTruthy();
  expect(screen.getByText("Ruch: —")).toBeTruthy();

  view.rerender(sessionBar(jan, false, true));
  expect(screen.getByText("Łączenie…")).toBeTruthy();
  expect(screen.queryByText(/^Brak danych z kasku/)).toBeNull();

  view.rerender(sessionBar({ ...jan, endedAt: jan.startedAt, status: "RESOLVED" }, false, false));
  expect(screen.getByText("Zakończona")).toBeTruthy();
  expect(screen.queryByText(/^Ruch/)).toBeNull();
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

it("okienka alarmów trzymają kolejność listy, a zamykane zostają do końca animacji", () => {
  const view = render(popups([alarm(1, 1, "SOS")]));

  view.rerender(popups([alarm(1, 1, "SOS"), alarm(2, 1, "NO_MOTION")]));
  expect(popupTitles()).toEqual(["Brak ruchu", "SOS"]);

  view.rerender(popups([alarm(2, 1, "NO_MOTION"), alarm(3, 1, "SOS")]));
  expect(popupTitles()).toEqual(["SOS", "Brak ruchu"]);

  view.rerender(popups([alarm(3, 1, "SOS")]));
  expect(popupTitles()).toEqual(["SOS", "Brak ruchu"]);
  expect(screen.getAllByRole("alert")[1].closest(".closing")).not.toBeNull();
});

it("dymek wykresu pokazuje czas i wartość z jednostką czujnika", () => {
  const point = { active: true, label: START, coordinate: undefined, accessibilityLayer: false, activeIndex: undefined };
  const view = render(<ChartTooltip {...point} payload={[{ graphicalItemId: "line", dataKey: "temperature", value: 33.26 }]} />);

  expect(screen.getByText("33,3 °C").closest(".tooltip.glass")).not.toBeNull();
  view.rerender(<ChartTooltip {...point} active={false} payload={[]} />);
  expect(view.container.textContent).toBe("");
});
