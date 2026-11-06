import type { AlarmGroup } from "./types";
import { alarmNames } from "./config";

export function alarmTitle(group: AlarmGroup) {
  if (group.ids.length > 1) {
    return `${alarmNames[group.alarmType]} ×${group.ids.length}`;
  }

  return alarmNames[group.alarmType];
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

export function errorText(error: string, updatedAt: number) {
  if (updatedAt === 0) {
    return error;
  }

  return `${error} · ostatnio ${formatTime(updatedAt)}`;
}

export function formatDay(value: string) {
  return new Date(value).toLocaleDateString("sv-SE");
}

export function formatClock(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${String(rest).padStart(2, "0")}`;
}

export function formatDelay(seconds: number) {
  return `−${formatClock(seconds)}`;
}

export function formatAge(milliseconds: number) {
  const seconds = Math.max(0, Math.round(milliseconds / 1000));

  if (seconds < 60) {
    return `${seconds} s`;
  }

  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)} min`;
  }

  return `${Math.floor(seconds / 3600)} h`;
}
