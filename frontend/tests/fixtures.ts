import { screen } from "@testing-library/react";
import type { Incident, Telemetry } from "../src/types";

export const jan: Incident = { id: 1, code: "Jan Kowalski", startedAt: "2026-09-21T10:00:00Z", endedAt: null, status: "IN_PROGRESS" };
export const START = Date.parse(jan.startedAt);

export function row(second: number, temperature: number | null): Telemetry {
  return {
    recordedAt: new Date(START + second * 1000).toISOString(),
    temperature,
    co2Ppm: null,
    tvocPpb: null,
    gasVoltage: null,
    motionState: null,
  };
}

export function cameraImage() {
  return screen.getByAltText("Obraz z kamery termowizyjnej");
}

export function imagePath() {
  return cameraImage().getAttribute("src");
}
