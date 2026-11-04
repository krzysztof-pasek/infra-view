import { useState } from "react";
import type { Incident, Telemetry, Threshold } from "./types";
import { MAX_REPLAY } from "./format";
import { CameraPanel } from "./CameraPanel";
import { Charts } from "./Charts";
import { ErrorLine } from "./ErrorLine";

type Props = {
  incident: Incident | undefined;
  rows: Telemetry[];
  thresholds: Threshold[] | null;
  error: string;
  updatedAt: number;
};

export function SessionView({ incident, rows, thresholds, error, updatedAt }: Props) {
  const [delay, setDelay] = useState(0);
  const [pausedAt, setPausedAt] = useState(0);
  const [frozenRows, setFrozenRows] = useState<Telemetry[]>([]);

  if (incident === undefined) {
    return <p className="card muted empty wide">Wybierz sesję z listy, aby zobaczyć obraz z kamery i pomiary</p>;
  }

  function seek(value: number) {
    setDelay(value);
    setPausedAt(0);
  }

  function pause() {
    setPausedAt(Date.now());
    setFrozenRows(rows);
  }

  function play() {
    let next = delay + Math.round((Date.now() - pausedAt) / 1000);

    if (next > MAX_REPLAY) {
      next = MAX_REPLAY;
    }

    seek(next);
  }

  let camera = <p className="card muted empty">Sesja zakończona, obraz na żywo jest niedostępny</p>;
  let chartsDelay = null;
  let chartsRows = rows;

  if (incident.status === "IN_PROGRESS") {
    camera = (
      <CameraPanel
        incidentId={incident.id}
        delay={delay}
        paused={pausedAt !== 0}
        onDelay={seek}
        onPause={pause}
        onPlay={play}
      />
    );
    chartsDelay = delay;

    if (pausedAt !== 0) {
      chartsRows = frozenRows;
    }
  }

  return (
    <>
      <section className="content" aria-label="Kamera">
        {camera}
      </section>

      <section className="content charts-column" aria-label="Wykresy">
        <ErrorLine error={error} updatedAt={updatedAt} />
        <Charts rows={chartsRows} thresholds={thresholds} delay={chartsDelay} />
      </section>
    </>
  );
}
