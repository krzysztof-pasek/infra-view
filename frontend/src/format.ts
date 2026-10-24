import type { AlarmType, MotionState } from "./types";

export const ONLINE_TIMEOUT = 5000;
export const MAX_REPLAY = 300;

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

export function formatDelay(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `−${minutes}:${String(rest).padStart(2, "0")}`;
}
