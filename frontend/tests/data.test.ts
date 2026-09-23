import { expect, it } from "vitest";
import { formatNumber, freshness } from "../src/format";
import { parseAlarm, parseIncident, parseTelemetry } from "../src/parsers";
import { prepareTelemetry } from "../src/telemetry";
import type { Telemetry } from "../src/types";

const sample: Telemetry = {
  id: 1,
  incidentId: 1,
  recordedAt: "2026-09-21T10:00:00Z",
  temperature: 0,
  gasPpm: null,
  co2Ppm: 430,
  motionState: null,
};

it("odróżnia zero od braku danych", () => {
  expect(formatNumber(0)).toBe("0");
  expect(formatNumber(null)).toBe("Brak danych");
});

it("rozpoznaje stary pomiar", () => {
  const minuteLater = Date.parse("2026-09-21T10:01:00Z");
  expect(freshness(sample.recordedAt, minuteLater)).toBe("Brak nowych pomiarów");
});

it("rozpoznaje pomiar z przyszłości", () => {
  const minuteEarlier = Date.parse("2026-09-21T09:59:00Z");
  expect(freshness(sample.recordedAt, minuteEarlier)).toBe("Sprawdź zegar urządzenia");
});

it("sortuje pomiary i usuwa duplikaty", () => {
  const later = { ...sample, id: 2, recordedAt: "2026-09-21T10:00:05Z" };
  const result = prepareTelemetry([later, sample, later], 1);
  expect(result.map(row => row.id)).toEqual([1, 2]);
});

it("pomija pomiary innej sesji", () => {
  const other = { ...sample, id: 2, incidentId: 2 };
  const result = prepareTelemetry([sample, other], 1);
  expect(result.map(row => row.id)).toEqual([1]);
});

it("zostawia tylko ostatnie 120 pomiarów", () => {
  const limit = 120;
  const start = Date.parse(sample.recordedAt);
  const rows = Array.from({ length: limit + 1 }, (_, i) => ({
    ...sample,
    id: i + 1,
    recordedAt: new Date(start + i * 1000).toISOString(),
  }));
  const oldest = rows[0];

  const result = prepareTelemetry(rows, 1);

  expect(result).toHaveLength(limit);
  expect(result).not.toContainEqual(oldest);
});

it("przyjmuje zero jako pomiar", () => {
  expect(parseTelemetry(sample).temperature).toBe(0);
});

it("odrzuca tekst zamiast liczby", () => {
  expect(() => parseTelemetry({ ...sample, temperature: "33" })).toThrow();
});

it("odrzuca nieznany typ alarmu", () => {
  const alarm = {
    id: 1,
    incidentId: 1,
    alarmType: "SOS",
    triggeredAt: sample.recordedAt,
    resolvedAt: null,
  };
  expect(() => parseAlarm(alarm)).toThrow();
});

it("odrzuca niekompletną sesję", () => {
  expect(() => parseIncident({ id: 1, status: "IN_PROGRESS" })).toThrow();
});