import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { Charts, pointsUntil, toPoints } from "../src/Charts";
import { CHART_WINDOW } from "../src/format";
import type { Telemetry } from "../src/types";

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

function temperature() {
  return screen.getByText("Temperatura").nextElementSibling?.textContent;
}

it("na żywo pokazuje ostatni pomiar", () => {
  render(<Charts rows={rows} thresholds={null} delay={0} />);

  expect(temperature()).toBe("40 °C");
});

it("pokazuje pomiar z chwili, w której jest cofnięte wideo", () => {
  render(<Charts rows={rows} thresholds={null} delay={10} />);

  expect(temperature()).toBe("30 °C");
});

it("nie pokazuje pomiaru sprzed początku nagrania", () => {
  render(<Charts rows={rows} thresholds={null} delay={60} />);

  expect(temperature()).toBe("— ");
});

it("w zakończonej sesji pokazuje ostatni pomiar", () => {
  render(<Charts rows={rows} thresholds={null} delay={null} />);

  expect(temperature()).toBe("40 °C");
});

it("okno wykresu nigdy nie wychodzi poza czas wideo", () => {
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
    const delay = random(320);
    const visible = pointsUntil(points, delay);
    const end = points[points.length - 1].time - delay * 1000;

    let before = 0;

    for (const point of points) {
      if (point.time <= end) {
        before++;
      }
    }

    expect(visible.length).toBe(Math.min(before, CHART_WINDOW));

    for (const point of visible) {
      expect(point.time).toBeLessThanOrEqual(end);
    }

    const earlier = pointsUntil(points, delay + 10);

    if (earlier.length > 0) {
      expect(earlier[earlier.length - 1].time).toBeLessThanOrEqual(visible[visible.length - 1].time);
    }
  }
});
