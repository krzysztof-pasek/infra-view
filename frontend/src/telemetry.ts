import type { Telemetry } from "./types";

export function prepareTelemetry(rows: Telemetry[], incidentId: number) {
  const unique = new Map<number, { row: Telemetry; time: number }>();

  for (const row of rows) {
    const time = Date.parse(row.recordedAt);

    if (row.incidentId === incidentId && Number.isFinite(time)) {
      unique.set(row.id, { row, time });
    }
  }

  return [...unique.values()]
    .sort((a, b) => a.time - b.time || a.row.id - b.row.id)
    .slice(-120)
    .map(item => item.row);
}