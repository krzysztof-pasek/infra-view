export type Incident = {
  id: number;
  code: string;
  startedAt: string;
  endedAt: string | null;
  status: "IN_PROGRESS" | "RESOLVED";
};

export type MotionState = "MOVING" | "STILL" | "NO_MOTION" | "FALLEN";

export type Telemetry = {
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
};

export type AlarmGroup = {
  key: string;
  incidentId: number;
  name: string;
  alarmType: AlarmType;
  ids: number[];
  latestId: number;
  triggeredAt: string;
};

export type Metric = "TEMPERATURE" | "CO2_PPM" | "TVOC_PPB";

export type Sensor = {
  key: "temperature" | "co2Ppm" | "tvocPpb" | "gasVoltage";
  metric: Metric | null;
  label: string;
  unit: string;
  digits: number;
};

export type Threshold = {
  metric: Metric;
  threshold: number;
  resetMargin: number;
  enabled: boolean;
};
