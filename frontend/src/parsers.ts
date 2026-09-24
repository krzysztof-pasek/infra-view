import type {
  Alarm,
  Incident,
  Recording,
  Telemetry,
} from "./types";

type Row = Record<string, unknown>;

function row(value: unknown): Row {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("Nieprawidłowy rekord API");
  }

  return value as Row;
}

function text(value: unknown): string {
  if (typeof value !== "string") {
    throw new Error("Nieprawidłowe pole tekstowe API");
  }

  return value;
}

function nullableText(value: unknown): string | null {
  return value === null ? null : text(value);
}

function number(value: unknown): number {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new Error("Nieprawidłowa liczba API");
  }

  return value;
}

function id(value: unknown): number {
  const result = number(value);

  if (!Number.isSafeInteger(result) || result <= 0) {
    throw new Error("Nieprawidłowe ID API");
  }

  return result;
}

function nullableNumber(value: unknown): number | null {
  return value === null ? null : number(value);
}

function date(value: unknown): string {
  const result = text(value);

  if (!Number.isFinite(Date.parse(result))) {
    throw new Error("Nieprawidłowa data API");
  }

  return result;
}

function nullableDate(value: unknown): string | null {
  return value === null ? null : date(value);
}

export function parseList<T>(
  value: unknown,
  parse: (item: unknown) => T
): T[] {
  if (!Array.isArray(value)) {
    throw new Error("API nie zwróciło listy");
  }

  return value.map(parse);
}

export function parseIncident(value: unknown): Incident {
  const item = row(value);

  if (item.status !== "IN_PROGRESS" && item.status !== "RESOLVED") {
    throw new Error("Nieznany status sesji");
  }

  return {
    id: id(item.id),
    code: text(item.code),
    firefighterName: nullableText(item.firefighterName),
    description: nullableText(item.description),
    location: nullableText(item.location),
    startedAt: date(item.startedAt),
    endedAt: nullableDate(item.endedAt),
    status: item.status,
  };
}

export function parseTelemetry(value: unknown): Telemetry {
  const item = row(value);

  return {
    id: id(item.id),
    incidentId: id(item.incidentId),
    recordedAt: date(item.recordedAt),
    temperature: nullableNumber(item.temperature),
    gasPpm: nullableNumber(item.gasPpm),
    co2Ppm: nullableNumber(item.co2Ppm),
    motionState: nullableText(item.motionState),
  };
}

export function parseAlarm(value: unknown): Alarm {
  const item = row(value);
  const type = item.alarmType;

  if (
    type !== "HIGH_TEMPERATURE" &&
    type !== "NO_MOTION" &&
    type !== "HIGH_GAS" &&
    type !== "FALL"
  ) {
    throw new Error("Nieznany typ alarmu API");
  }

  return {
    id: id(item.id),
    incidentId: id(item.incidentId),
    alarmType: type,
    triggeredAt: date(item.triggeredAt),
    resolvedAt: nullableDate(item.resolvedAt),
  };
}

export function parseRecording(value: unknown): Recording {
  const item = row(value);

  return {
    id: id(item.id),
    incidentId: id(item.incidentId),
    startedAt: date(item.startedAt),
    endedAt: nullableDate(item.endedAt),
    filePath: nullableText(item.filePath),
    fileSizeBytes: nullableNumber(item.fileSizeBytes),
    durationSec: nullableNumber(item.durationSec),
  };
}