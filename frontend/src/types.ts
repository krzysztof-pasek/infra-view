export interface Incident {
  id: number;
  code: string;
  firefighterName: string | null;
  description: string | null;
  location: string | null;
  startedAt: string;
  endedAt: string | null;
  status: "IN_PROGRESS" | "RESOLVED";
}

export type NewIncident = Omit<Incident, "id">;

export interface Telemetry {
  id: number;
  incidentId: number;
  recordedAt: string;
  temperature: number | null;
  gasPpm: number | null;
  co2Ppm: number | null;
  motionState: string | null;
}

export interface Alarm {
  id: number;
  incidentId: number;
  alarmType: "HIGH_TEMPERATURE" | "NO_MOTION" | "HIGH_GAS" | "FALL";
  triggeredAt: string;
  resolvedAt: string | null;
}

export interface Recording {
  id: number;
  incidentId: number;
  startedAt: string;
  endedAt: string | null;
  filePath: string | null;
  fileSizeBytes: number | null;
  durationSec: number | null;
}