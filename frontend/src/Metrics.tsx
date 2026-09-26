import type { Telemetry } from "./types";
import { formatDate, formatNumber, freshness } from "./format";

type Props = {
  latest: Telemetry | undefined;
  now: number;
  ended: boolean;
};

export function Metrics({ latest, now, ended }: Props) {
  const fields = [
    { label: "Temperatura", value: latest?.temperature, unit: "°C" },
    { label: "Gaz (gasPpm)", value: latest?.gasPpm, unit: "ppm" },
    { label: "CO₂ / eCO₂", value: latest?.co2Ppm, unit: "ppm" },
  ];

  return (
    <section className="panel" aria-label="Pomiary środowiskowe">
      <div className="row">
        <h2>Pomiary</h2>

        <span className="badge">
          {!latest ? "Brak pomiarów" : ended ? "Dane historyczne" : freshness(latest.recordedAt, now)}
        </span>
      </div>

      <div className="metrics">
        {fields.map(field => (
          <div className="metric" key={field.label}>
            <span className="muted">{field.label}</span>
            <strong>{formatNumber(field.value)}</strong>
            <small>{field.unit}</small>
          </div>
        ))}
      </div>

      <p>
        Aktywność: {latest?.motionState || "Nieustalona"}
      </p>

      <small>
        Ostatni pomiar: {formatDate(latest?.recordedAt)}
      </small>
    </section>
  );
}