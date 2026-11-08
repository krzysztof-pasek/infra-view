import { useEffect, useState } from "react";
import type { Incident, Telemetry, Threshold } from "./types";
import { lastItem } from "./data";
import { animationTime, MAX_REPLAY_SECONDS } from "./config";
import { formatClock, formatDelay } from "./format";
import { CameraPanel } from "./CameraPanel";
import { Charts } from "./Charts";
import { ErrorLine } from "./ErrorLine";

type Props = {
  incident: Incident | null;
  rows: Telemetry[];
  thresholds: Threshold[] | null;
  error: string;
  updatedAt: number;
  now: number;
  online: boolean;
};

function recordingLength(incident: Incident | null) {
  if (incident === null || incident.endedAt === null) {
    return 0;
  }

  return Math.round((Date.parse(incident.endedAt) - Date.parse(incident.startedAt)) / 1000);
}

export function SessionView({ incident, rows, thresholds, error, updatedAt, now, online }: Props) {
  const [delay, setDelay] = useState(recordingLength(incident));
  const [playedAt, setPlayedAt] = useState(Date.now);
  const [pausedAt, setPausedAt] = useState(0);
  const [frozenRows, setFrozenRows] = useState<Telemetry[]>([]);
  const [seeking, setSeeking] = useState(false);
  const [connection, setConnection] = useState(0);
  const [wasConnected, setWasConnected] = useState(false);
  const connected = online && error === "";

  if (connected !== wasConnected) {
    setWasConnected(connected);
    setConnection(connection + 1);
  }

  useEffect(() => {
    if (!seeking) {
      return;
    }

    const timer = setTimeout(() => setSeeking(false), animationTime());
    return () => clearTimeout(timer);
  }, [seeking]);

  if (incident === null) {
    return <p className="card muted empty wide">Wybierz sesję z listy, aby zobaczyć obraz z kamery i pomiary</p>;
  }

  const live = incident.status === "IN_PROGRESS";
  const base = `/api/incidents/${incident.id}`;
  let max = MAX_REPLAY_SECONDS;
  let current = delay;
  let paused = pausedAt !== 0;
  let path = `${base}/live?connection=${connection}`;
  let label: string | null = null;
  let chartsRows = rows;
  let until = 0;
  let span = MAX_REPLAY_SECONDS;
  let frameTime = pausedAt - delay * 1000;

  if (live) {
    const age = Math.round((now - Date.parse(incident.startedAt)) / 1000);

    if (age < max) {
      max = age;
    }

    if (max < 0) {
      max = 0;
    }

    span = max;

    if (delay > 0) {
      path = `${base}/replay?delaySeconds=${delay}&connection=${connection}`;
      label = formatDelay(delay);
    }

    if ((!online && delay === 0) || error !== "") {
      path = "";
    }

    if (paused) {
      chartsRows = frozenRows;
    }

    const lastRow = lastItem(chartsRows);

    if (lastRow !== null) {
      until = Date.parse(lastRow.recordedAt) - delay * 1000;
    }
  }

  function position(time: number) {
    let played = Math.round((time - playedAt) / 1000);

    if (played < 0 || pausedAt !== 0) {
      played = 0;
    }

    if (played > delay) {
      played = delay;
    }

    return delay - played;
  }

  if (!live) {
    max = recordingLength(incident);
    const end = Date.parse(incident.startedAt) + max * 1000;
    current = position(now);
    path = `${base}/recording?from=${new Date(end - delay * 1000).toISOString()}`;
    label = formatClock(max - current);
    until = end - current * 1000;
    frameTime = until;

    if (current === 0) {
      paused = true;
    }
  }

  if (paused) {
    path = `${base}/frame?at=${new Date(frameTime).toISOString()}`;
  }

  function seek(value: number) {
    setDelay(value);
    setPlayedAt(Date.now());
    setPausedAt(0);
    setSeeking(true);
  }

  function pause() {
    if (!live) {
      setDelay(position(Date.now()));
    }

    setPausedAt(Date.now());
    setFrozenRows(rows);
  }

  function play() {
    let next = current;

    if (live) {
      next = delay + Math.round((Date.now() - pausedAt) / 1000);
    }

    if (!live && current === 0) {
      next = max;
    }

    if (next > max) {
      next = max;
    }

    seek(next);
  }

  return (
    <>
      <section className="content" aria-label="Kamera">
        <CameraPanel
          path={path}
          live={live}
          paused={paused}
          max={max}
          delay={current}
          label={label}
          onSeek={seek}
          onPause={pause}
          onPlay={play}
        />
      </section>

      <section className="content charts-column" aria-label="Wykresy">
        <ErrorLine error={error} updatedAt={updatedAt} />
        <Charts rows={chartsRows} thresholds={thresholds} until={until} span={span} animate={seeking} />
      </section>
    </>
  );
}
