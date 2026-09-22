import type { Telemetry } from "./types";

export function prepareTelemetry(rows: Telemetry[], incidentId: number) {
  const unique = new Map<number, Telemetry>();

  for (const row of rows) {
    if (
      row.incidentId === incidentId &&
      Number.isFinite(Date.parse(row.recordedAt))
    ) {
      unique.set(row.id, row);
    }
  }

  return [...unique.values()]
    .sort(
      (a, b) =>
        Date.parse(a.recordedAt) - Date.parse(b.recordedAt) ||
        a.id - b.id
    )
    .slice(-120);
}