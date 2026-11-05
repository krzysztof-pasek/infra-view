import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Charts, pointsUntil, toPoints } from "../src/Charts";
import { SessionView } from "../src/SessionView";
import { MAX_REPLAY } from "../src/format";
import type { Incident, Telemetry } from "../src/types";

const START = Date.parse("2026-09-21T10:00:00Z");

function row(second: number, temperature: number): Telemetry {
  return {
    recordedAt: new Date(START + second * 1000).toISOString(),
    temperature,
    co2Ppm: null,
    tvocPpb: null,
    gasVoltage: null,
    motionState: null,
  };
}

const rows = [row(0, 20), row(10, 30), row(20, 40)];

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
  render(<Charts rows={rows} thresholds={null} until={START + 20000} span={MAX_REPLAY} animate={false} />);
  expect(temperature()).toBe("40 °C");

  render(<Charts rows={rows} thresholds={null} until={START + 10000} span={MAX_REPLAY} animate={false} />);
  expect(valueAfter(screen.getAllByText("Temperatura")[1])).toBe("30 °C");
});

it("nie pokazuje pomiaru sprzed początku nagrania", () => {
  render(<Charts rows={rows} thresholds={null} until={START - 1000} span={MAX_REPLAY} animate={false} />);

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

    for (let i = 0; i < 1 + random(400); i++) {
      second += 1 + random(10);
      many.push(row(second, 20));
    }

    const points = toPoints(many);
    const end = points[points.length - 1].time - random(320) * 1000;
    const span = 1 + random(MAX_REPLAY);
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
  const session: Incident = { id: 1, code: "Jan Kowalski", startedAt: rows[0].recordedAt, endedAt: null, status: "IN_PROGRESS" };
  const view = render(<SessionView incident={session} rows={rows.slice(0, 2)} thresholds={null} error="" updatedAt={0} online now={Date.now()} />);

  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));
  const ended: Incident = { ...session, endedAt: rows[2].recordedAt, status: "RESOLVED" };
  view.rerender(<SessionView incident={ended} rows={rows} thresholds={null} error="" updatedAt={0} online now={Date.now()} />);

  expect(temperature()).toBe("40 °C");
});

it("nagranie zakończonej sesji przesuwa wykresy razem z wideo i zatrzymuje się na końcu", () => {
  vi.useFakeTimers({ toFake: ["Date"] });
  const ended: Incident = { id: 1, code: "Jan Kowalski", startedAt: rows[0].recordedAt, endedAt: rows[2].recordedAt, status: "RESOLVED" };

  function show(seconds: number) {
    vi.setSystemTime(START + seconds * 1000);
    return <SessionView incident={ended} rows={rows} thresholds={null} error="" updatedAt={0} online now={Date.now()} />;
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

  fireEvent.click(screen.getByRole("button", { name: "Wznów" }));
  view.rerender(show(100));
  expect(temperature()).toBe("40 °C");
  expect(screen.getByText("0:20")).toBeTruthy();
  expect(screen.getByRole("button", { name: "Wznów" })).toBeTruthy();
  vi.useRealTimers();
});

it("na żywo pasek cofania obejmuje tylko czas trwania sesji, do 5 minut", () => {
  const now = Date.now();
  const young: Incident = { id: 1, code: "Jan Kowalski", startedAt: new Date(now - 30000).toISOString(), endedAt: null, status: "IN_PROGRESS" };
  const view = render(<SessionView incident={young} rows={[]} thresholds={null} error="" updatedAt={0} online now={now} />);
  const slider = screen.getByLabelText("Przewijanie obrazu") as HTMLInputElement;

  expect(slider.max).toBe("30");
  view.rerender(<SessionView incident={young} rows={[]} thresholds={null} error="" updatedAt={0} online now={now + 15000} />);
  expect(slider.max).toBe("45");

  const old: Incident = { ...young, startedAt: new Date(now - 1000000).toISOString() };
  view.rerender(<SessionView incident={old} rows={[]} thresholds={null} error="" updatedAt={0} online now={now} />);
  expect(slider.max).toBe("300");
});

it("na żywo nie ma podpisu czasu, pojawia się dopiero po cofnięciu", async () => {
  const now = Date.now();
  const session: Incident = { id: 1, code: "Jan Kowalski", startedAt: new Date(now - 30000).toISOString(), endedAt: null, status: "IN_PROGRESS" };
  render(<SessionView incident={session} rows={[]} thresholds={null} error="" updatedAt={0} online now={now} />);
  const slider = screen.getByLabelText("Przewijanie obrazu");

  expect(screen.queryByText(/^−?\d+:\d\d$/)).toBeNull();
  fireEvent.change(slider, { target: { value: "10" } });
  expect(screen.getByText("−0:20")).toBeTruthy();

  fireEvent.click(screen.getByRole("button", { name: "Na żywo" }));
  await waitFor(() => expect(screen.queryByText("−0:20")).toBeNull());
});
