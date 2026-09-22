import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import type { Telemetry } from "./types";

type Field = "temperature" | "gasPpm" | "co2Ppm";

type ChartRow = {
  time: number;
  temperature: number | null;
  gasPpm: number | null;
  co2Ppm: number | null;
};

type ChartProps = {
  rows: Telemetry[];
  field: Field;
  label: string;
  unit: string;
};

type ChartsProps = {
  rows: Telemetry[];
};

function chartRows(rows: Telemetry[]) {
  const result: ChartRow[] = [];

  rows.forEach((row, index) => {
    const time = Date.parse(row.recordedAt);
    const previous = rows[index - 1];

    if (previous) {
      const previousTime = Date.parse(previous.recordedAt);

      if (time - previousTime > 5000) {
        result.push({
          time: previousTime + 1,
          temperature: null,
          gasPpm: null,
          co2Ppm: null,
        });
      }
    }

    result.push({
      time,
      temperature: row.temperature,
      gasPpm: row.gasPpm,
      co2Ppm: row.co2Ppm,
    });
  });

  return result;
}

function MeasurementChart({ rows, field, label, unit }: ChartProps) {
  const data = chartRows(rows);
  const hasData = rows.some(row => row[field] !== null);

  return (
    <section className="panel">
      <h3>
        {label} ({unit})
      </h3>

      {!hasData ? (
        <p className="muted">Brak danych do wykresu</p>
      ) : (
        <div
          className="chart"
          role="img"
          aria-label={`Historia: ${label}`}
        >
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={data}
              margin={{ top: 8, right: 12, bottom: 4, left: 0 }}
            >
              <CartesianGrid
                stroke="var(--border)"
                strokeDasharray="3 3"
              />

              <XAxis
                dataKey="time"
                type="number"
                domain={["dataMin", "dataMax"]}
                tickFormatter={value =>
                  new Date(value).toLocaleTimeString("pl-PL")
                }
                tick={{ fill: "var(--muted)", fontSize: 11 }}
                minTickGap={30}
              />

              <YAxis
                width={45}
                tick={{ fill: "var(--muted)", fontSize: 11 }}
              />

              <Tooltip
                labelFormatter={value =>
                  new Date(Number(value)).toLocaleString("pl-PL")
                }
                contentStyle={{
                  background: "var(--solid)",
                  borderColor: "var(--border)",
                  color: "var(--text)",
                }}
              />

              <Line
                dataKey={field}
                name={label}
                unit={` ${unit}`}
                type="linear"
                stroke="var(--accent)"
                strokeWidth={2}
                dot={rows.length === 1}
                connectNulls={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}

export function Charts({ rows }: ChartsProps) {
  return (
    <div className="charts">
      <MeasurementChart
        rows={rows}
        field="temperature"
        label="Temperatura"
        unit="°C"
      />

      <MeasurementChart
        rows={rows}
        field="gasPpm"
        label="Gaz"
        unit="ppm"
      />

      <MeasurementChart
        rows={rows}
        field="co2Ppm"
        label="CO₂ / eCO₂"
        unit="ppm"
      />
    </div>
  );
}