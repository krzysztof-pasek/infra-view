export type Incident = {
  id: number;
  code: string;
  firefighterName: string | null;
  startedAt: string;
  endedAt: string | null;
  status: "IN_PROGRESS" | "RESOLVED";
};

export type MotionState = "MOVING" | "STILL" | "NO_MOTION" | "FALLEN";

export type Telemetry = {
  id: number;
  recordedAt: string;
  temperature: number | null;
  co2Ppm: number | null;
  tvocPpb: number | null;
  gasVoltage: number | null;
  motionState: MotionState | null;
};

export type AlarmType = "HIGH_TEMPERATURE" | "HIGH_CO2" | "HIGH_TVOC" | "NO_MOTION" | "FALL" | "SOS";

export type Alarm = {
  id: number;
  incidentId: number;
  alarmType: AlarmType;
  triggeredAt: string;
  resolvedAt: string | null;
};

export type AlarmGroup = {
  key: string;
  incidentId: number;
  alarmType: AlarmType;
  ids: number[];
  latestId: number;
  triggeredAt: string;
};

export type Metric = "TEMPERATURE" | "CO2_PPM" | "TVOC_PPB";

export type Threshold = {
  metric: Metric;
  alarmType: AlarmType;
  threshold: number;
  resetMargin: number;
  enabled: boolean;
};
