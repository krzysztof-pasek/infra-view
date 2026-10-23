import type { Alarm, AlarmGroup, Incident, Telemetry } from "./types";

export function incidentName(incident: Incident) {
  if (incident.firefighterName === null || incident.firefighterName === "") {
    return incident.code;
  }

  return incident.firefighterName;
}

export function findIncident(incidents: Incident[], id: number | null) {
  for (const incident of incidents) {
    if (incident.id === id) {
      return incident;
    }
  }

  return undefined;
}

export function nameOf(incidents: Incident[], id: number) {
  const incident = findIncident(incidents, id);

  if (incident === undefined) {
    return `Sesja ${id}`;
  }

  return incidentName(incident);
}

export function sortByTime(rows: Telemetry[]) {
  const sorted = [...rows];
  sorted.sort((a, b) => Date.parse(a.recordedAt) - Date.parse(b.recordedAt));
  return sorted;
}

export function groupAlarms(alarms: Alarm[]) {
  const groups: AlarmGroup[] = [];

  for (const alarm of alarms) {
    if (alarm.resolvedAt !== null) {
      continue;
    }

    const key = alarm.incidentId + "-" + alarm.alarmType;
    let group: AlarmGroup | null = null;

    for (const existing of groups) {
      if (existing.key === key) {
        group = existing;
      }
    }

    if (group === null) {
      group = {
        key,
        incidentId: alarm.incidentId,
        alarmType: alarm.alarmType,
        ids: [],
        latestId: alarm.id,
        triggeredAt: alarm.triggeredAt,
      };
      groups.push(group);
    }

    group.ids.push(alarm.id);

    if (alarm.id > group.latestId) {
      group.latestId = alarm.id;
      group.triggeredAt = alarm.triggeredAt;
    }
  }

  groups.sort((a, b) => b.latestId - a.latestId);
  return groups;
}
