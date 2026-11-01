import type { Alarm, AlarmGroup, Incident, Metric, Threshold } from "./types";

export function findIncident(incidents: Incident[], id: number | null) {
  for (const incident of incidents) {
    if (incident.id === id) {
      return incident;
    }
  }

  return undefined;
}

export function findThreshold(thresholds: Threshold[] | null, metric: Metric | null) {
  if (thresholds === null) {
    return null;
  }

  for (const threshold of thresholds) {
    if (threshold.metric === metric) {
      return threshold;
    }
  }

  return null;
}

export function groupAlarms(alarms: Alarm[], incidents: Incident[]) {
  const groups: AlarmGroup[] = [];

  for (const alarm of alarms) {
    const key = alarm.incidentId + "-" + alarm.alarmType;
    let group: AlarmGroup | null = null;

    for (const existing of groups) {
      if (existing.key === key) {
        group = existing;
      }
    }

    if (group === null) {
      const incident = findIncident(incidents, alarm.incidentId);
      let name = `Sesja ${alarm.incidentId}`;

      if (incident !== undefined) {
        name = incident.code;
      }

      group = {
        key,
        incidentId: alarm.incidentId,
        name,
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
