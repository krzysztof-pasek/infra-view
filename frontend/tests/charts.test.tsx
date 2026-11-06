import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Charts, pointsUntil, toPoints } from "../src/Charts";
import { SessionView } from "../src/SessionView";
import { MAX_REPLAY_SECONDS } from "../src/config";
import type { Incident, Telemetry } from "../src/types";
import { imagePath, jan, row, START } from "./fixtures";

const rows = [row(0, 20), row(10, 30), row(20, 40)];
const ended: Incident = { ...jan, endedAt: rows[2].recordedAt, status: "RESOLVED" };

function charts(until: number) {
  return <Charts rows={rows} thresholds={null} until={until} span={MAX_REPLAY_SECONDS} animate={false} />;
}

function sessionView(incident: Incident, shownRows: Telemetry[], now: number) {
  return <SessionView incident={incident} rows={shownRows} thresholds={null} error="" updatedAt={0} online now={now} />;
}

function young(now: number): Incident {
  return { ...jan, startedAt: new Date(now - 30000).toISOString() };
}

function valueAfter(title: Element) {
  const value = title.nextElementSibling;

  if (value === null) {
    return null;
  }

  return value.textContent;
}

function temperature() {
  return valueAfter(screen.getByText("Temperatura"));
}

it("pokazuje ostatni pomiar sprzed czasu wideo", () => {
  render(charts(START + 20000));
  expect(temperature()).toBe("40 °C");

  render(charts(START + 10000));
  expect(valueAfter(screen.getAllByText("Temperatura")[1])).toBe("30 °C");
});

it("nie pokazuje pomiaru sprzed początku nagrania", () => {
  render(charts(START - 1000));

  expect(temperature()).toBe("— ");
});

it("okno wykresu obejmuje tylko zakres paska i nigdy nie wychodzi poza czas wideo", () => {
  let seed = 1;

  function random(max: number) {
    seed = (seed * 16807) % 2147483647;
    return seed % max;
  }

  for (let run = 0; run < 500; run++) {
    const many: Telemetry[] = [];
    let second = 0;

    for (let index = 0; index < 1 + random(400); index++) {
      second += 1 + random(10);
      many.push(row(second, 20));
    }

    const points = toPoints(many);
    const end = points[points.length - 1].time - random(320) * 1000;
    const span = 1 + random(MAX_REPLAY_SECONDS);
    const visible = pointsUntil(points, end, span);

    let inside = 0;

    for (const point of points) {
      if (point.time <= end && point.time > end - span * 1000) {
        inside++;
      }
    }

    expect(visible.length).toBe(inside);

    for (const point of visible) {
      expect(point.time).toBeLessThanOrEqual(end);
      expect(point.time).toBeGreaterThan(end - span * 1000);
    }

    const earlier = pointsUntil(points, end - 10000, span);

    if (earlier.length > 0) {
      expect(earlier[earlier.length - 1].time).toBeLessThanOrEqual(visible[visible.length - 1].time);
    }
  }
});

it("po zakończeniu sesji wykres nie zostaje zamrożony na pauzie", () => {
  const view = render(sessionView(jan, rows.slice(0, 2), Date.now()));

  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));
  view.rerender(sessionView(ended, rows, Date.now()));

  expect(temperature()).toBe("40 °C");
});

it("nagranie zakończonej sesji przesuwa wykresy razem z wideo i zatrzymuje się na końcu", () => {
  vi.useFakeTimers({ toFake: ["Date"] });

  function show(seconds: number) {
    vi.setSystemTime(START + seconds * 1000);
    return sessionView(ended, rows, Date.now());
  }

  const view = render(show(0));
  expect(temperature()).toBe("20 °C");
  expect(screen.getByText("0:00")).toBeTruthy();

  view.rerender(show(10));
  expect(temperature()).toBe("30 °C");
  expect(screen.getByText("0:10")).toBeTruthy();

  vi.setSystemTime(START + 11000);
  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));
  view.rerender(show(40));
  expect(screen.getByText("0:11")).toBeTruthy();
  expect(imagePath()).toBe("/api/incidents/1/frame?at=2026-09-21T10:00:11.000Z");

  fireEvent.click(screen.getByRole("button", { name: "Wznów" }));
  view.rerender(show(100));
  expect(temperature()).toBe("40 °C");
  expect(screen.getByText("0:20")).toBeTruthy();
  expect(screen.getByRole("button", { name: "Wznów" })).toBeTruthy();
});

it("na żywo pasek cofania obejmuje tylko czas trwania sesji, do 5 minut", () => {
  const now = Date.now();
  const view = render(sessionView(young(now), [], now));
  const slider = screen.getByLabelText<HTMLInputElement>("Przewijanie obrazu");

  expect(slider.max).toBe("30");
  view.rerender(sessionView(young(now), [], now + 15000));
  expect(slider.max).toBe("45");

  view.rerender(sessionView({ ...jan, startedAt: new Date(now - 1000000).toISOString() }, [], now));
  expect(slider.max).toBe("300");
});

it("na żywo nie ma podpisu czasu, pojawia się dopiero po cofnięciu", async () => {
  const now = Date.now();
  render(sessionView(young(now), [], now));

  expect(screen.queryByText(/^−?\d+:\d\d$/)).toBeNull();
  fireEvent.change(screen.getByLabelText("Przewijanie obrazu"), { target: { value: "10" } });
  expect(screen.getByText("−0:20")).toBeTruthy();

  fireEvent.click(screen.getByRole("button", { name: "Na żywo" }));
  await waitFor(() => expect(screen.queryByText("−0:20")).toBeNull());
});

it("pauza po cofnięciu pokazuje klatkę z chwili, którą było widać", () => {
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(START + 30000);
  render(sessionView(jan, [], Date.now()));

  fireEvent.change(screen.getByLabelText("Przewijanie obrazu"), { target: { value: "10" } });
  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));

  expect(imagePath()).toBe("/api/incidents/1/frame?at=2026-09-21T10:00:10.000Z");
});
