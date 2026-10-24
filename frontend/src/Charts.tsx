import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { Telemetry } from "./types";
import type { Sensor } from "./format";
import { formatDate, formatNumber, formatTime, GAP_TIMEOUT, sensors, sensorValue } from "./format";

type Point = {
  time: number;
  temperature: number | null;
  co2Ppm: number | null;
  tvocPpb: number | null;
  gasVoltage: number | null;
};

function toPoints(rows: Telemetry[]) {
  const points: Point[] = [];

  for (const row of rows) {
    const time = Date.parse(row.recordedAt);
    const last = points.at(-1);

    if (last !== undefined && time - last.time > GAP_TIMEOUT) {
      points.push({ time: last.time + 1, temperature: null, co2Ppm: null, tvocPpb: null, gasVoltage: null });
    }

    points.push({
      time,
      temperature: row.temperature,
      co2Ppm: row.co2Ppm,
      tvocPpb: row.tvocPpb,
      gasVoltage: row.gasVoltage,
    });
  }

  return points;
}

type Props = {
  rows: Telemetry[];
};

export function Charts({ rows }: Props) {
  if (rows.length === 0) {
    return <p className="card muted">Brak pomiarów</p>;
  }

  const points = toPoints(rows);
  const latest = rows[rows.length - 1];

  return (
    <>
      {sensors.map(sensor => (
        <SensorChart
          key={sensor.key}
          sensor={sensor}
          points={points}
          value={sensorValue(latest, sensor)}
        />
      ))}
    </>
  );
}

type ChartProps = {
  sensor: Sensor;
  points: Point[];
  value: number | null;
};

function SensorChart({ sensor, points, value }: ChartProps) {
  const axis = { fill: "var(--muted)", fontSize: 11 };
  let unit = null;

  if (value !== null) {
    unit = <small>{sensor.unit}</small>;
  }

  return (
    <section className="card">
      <div className="chart-header">
        <h3>{sensor.label}</h3>
        <strong className="value">
          {formatNumber(value, sensor.digits)} {unit}
        </strong>
      </div>

      <div className="chart">
        <ResponsiveContainer>
          <LineChart data={points} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid stroke="var(--line)" vertical={false} />
            <XAxis
              dataKey="time"
              type="number"
              domain={["dataMin", "dataMax"]}
              tickFormatter={formatTime}
              tick={axis}
              stroke="var(--line)"
              minTickGap={40}
            />
            <YAxis
              width={44}
              domain={["auto", "auto"]}
              tick={axis}
              axisLine={false}
              tickLine={false}
              tickFormatter={value => formatNumber(value, sensor.digits)}
            />
            <Tooltip
              labelFormatter={time => formatDate(Number(time))}
              contentStyle={{ background: "var(--solid)", border: "none", borderRadius: 12 }}
            />
            <Line
              dataKey={sensor.key}
              name={sensor.label}
              unit={" " + sensor.unit}
              stroke="var(--accent)"
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
