import type { AlarmType, Metric, MotionState, Telemetry } from "./types";

export const ONLINE_TIMEOUT = 5000;
export const GAP_TIMEOUT = 5000;
export const CHART_WINDOW = 120;
export const MAX_REPLAY = 300;

export type Sensor = {
  key: "temperature" | "co2Ppm" | "tvocPpb" | "gasVoltage";
  metric: Metric | null;
  label: string;
  unit: string;
  digits: number;
};

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
};

export function sensorValue(row: Telemetry | undefined, sensor: Sensor) {
  if (row === undefined) {
    return null;
  }

  return row[sensor.key];
}

export function formatNumber(value: number | null, digits: number) {
  if (value === null) {
    return "—";
  }

  return value.toLocaleString("pl-PL", { maximumFractionDigits: digits });
}

export function formatDate(value: string | number) {
  return new Date(value).toLocaleString("pl-PL");
}

export function formatTime(value: string | number) {
  return new Date(value).toLocaleTimeString("pl-PL");
}

export function formatDay(value: string) {
  return new Date(value).toLocaleDateString("sv-SE");
}

export function formatDelay(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `−${minutes}:${String(rest).padStart(2, "0")}`;
}
