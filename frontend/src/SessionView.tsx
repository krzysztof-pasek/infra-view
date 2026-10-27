import type { Incident, Telemetry, Threshold } from "./types";
import { errorText } from "./format";
import { CameraPanel } from "./CameraPanel";
import { Charts } from "./Charts";

type Props = {
  incident: Incident | undefined;
  rows: Telemetry[];
  thresholds: Threshold[] | null;
  error: string;
  updatedAt: number;
};

export function SessionView({ incident, rows, thresholds, error, updatedAt }: Props) {
  if (incident === undefined) {
    return <p className="card muted empty wide">Wybierz sesję z listy, aby zobaczyć obraz z kamery i pomiary</p>;
  }

  let camera = <p className="card muted empty">Sesja zakończona, obraz na żywo jest niedostępny</p>;
  let telemetryError = null;

  if (incident.status === "IN_PROGRESS") {
    camera = <CameraPanel key={incident.id} incidentId={incident.id} />;
  }

  if (error !== "") {
    telemetryError = <p className="orange">{errorText(error, updatedAt)}</p>;
  }

  return (
    <>
      <section className="content" aria-label="Kamera">
        {camera}
      </section>

      <section className="content charts-column" aria-label="Wykresy">
        {telemetryError}
        <Charts key={incident.id} rows={rows} thresholds={thresholds} />
      </section>
    </>
  );
}
