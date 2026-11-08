import type { AlarmType, MotionState, Sensor } from "./types";

export const ONLINE_TIMEOUT = 5000;
export const GAP_TIMEOUT = 5000;
export const RETRY_TIMEOUT = 3000;
export const MAX_REPLAY_SECONDS = 300;

export const sensors: Sensor[] = [
  { key: "temperature", metric: "TEMPERATURE", label: "Temperatura", unit: "°C", digits: 1 },
  { key: "co2Ppm", metric: "CO2_PPM", label: "CO₂", unit: "ppm", digits: 0 },
  { key: "tvocPpb", metric: "TVOC_PPB", label: "Związki organiczne (TVOC)", unit: "ppb", digits: 0 },
  { key: "gasVoltage", metric: null, label: "CO i gazy palne (intensywność)", unit: "V", digits: 2 },
];

export const motionStates: Record<MotionState, { label: string; color: string }> = {
  MOVING: { label: "W ruchu", color: "" },
  STILL: { label: "Nieruchomo", color: "" },
  NO_MOTION: { label: "Brak ruchu", color: "red" },
  FALLEN: { label: "Upadek", color: "red" },
};

export const alarmNames: Record<AlarmType, string> = {
  HIGH_TEMPERATURE: "Wysoka temperatura",
  HIGH_CO2: "Wysokie CO₂",
  HIGH_TVOC: "Wysokie TVOC",
  NO_MOTION: "Brak ruchu",
  FALL: "Upadek",
  SOS: "SOS",
};

export function animationTime() {
  let time = parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--time")) * 1000;

  if (Number.isNaN(time)) {
    time = 0;
  }

  return time;
}
