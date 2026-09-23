import { request } from "./http";
import {
  parseAlarm,
  parseIncident,
  parseList,
  parseRecording,
  parseTelemetry,
} from "./parsers";

import type { NewIncident } from "./types";

export async function getIncidents(signal: AbortSignal) {
  return parseList(
    await request("/incidents", { signal }),
    parseIncident
  );
}

export async function getAlarms(signal: AbortSignal) {
  return parseList(
    await request("/alarms", { signal }),
    parseAlarm
  );
}

export async function getTelemetry(
  incidentId: number,
  signal: AbortSignal
) {
  return parseList(
    await request(`/telemetry/incident/${incidentId}`, { signal }),
    parseTelemetry
  );
}

export async function getRecordings(
  incidentId: number,
  signal: AbortSignal
) {
  const recordings = parseList(
    await request(`/videos/incident/${incidentId}`, { signal }),
    parseRecording
  );

  return recordings.filter(
    recording => recording.incidentId === incidentId
  );
}

export async function createIncident(input: NewIncident) {
  return parseIncident(
    await request("/incidents", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(input),
    })
  );
}

export async function endIncident(id: number) {
  return parseIncident(
    await request(`/incidents/${id}/end`, {
      method: "PUT",
    })
  );
}

export async function resolveAlarm(id: number) {
  return parseAlarm(
    await request(`/alarms/${id}/resolve`, {
      method: "PUT",
    })
  );
}