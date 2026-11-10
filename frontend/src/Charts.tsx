import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { TooltipContentProps } from "recharts";
import type { Sensor, Telemetry, Threshold } from "./types";
import { animationTime, GAP_TIMEOUT, sensors } from "./config";
import { formatDate, formatNumber, formatTime } from "./format";
import { findThreshold, lastItem } from "./data";

type Point = {
  time: number;
  temperature: number | null;
  co2Ppm: number | null;
  tvocPpb: number | null;
  gasVoltage: number | null;
};

function makePoint(time: number, row: Telemetry | null) {
  const point: Point = { time, temperature: null, co2Ppm: null, tvocPpb: null, gasVoltage: null };

  if (row !== null) {
    for (const sensor of sensors) {
      point[sensor.key] = row[sensor.key];
    }
  }

  return point;
}

export function toPoints(rows: Telemetry[]) {
  const points: Point[] = [];

  for (const row of rows) {
    const time = Date.parse(row.recordedAt);
    const last = lastItem(points);

    if (last !== null && time - last.time > GAP_TIMEOUT) {
      points.push(makePoint(last.time + 1, null));
    }

    points.push(makePoint(time, row));
  }

  return points;
}

export function pointsUntil(points: Point[], until: number, span: number) {
  const visible: Point[] = [];

  for (const point of points) {
    if (point.time <= until && point.time > until - span * 1000) {
      visible.push(point);
    }
  }

  return visible;
}

type Props = {
  rows: Telemetry[];
  thresholds: Threshold[] | null;
  until: number;
  span: number;
  animate: boolean;
};

export function Charts({ rows, thresholds, until, span, animate }: Props) {
  if (rows.length === 0) {
    return <p className="card muted">Brak pomiarów</p>;
  }

  const visible = pointsUntil(toPoints(rows), until, span);
  let current = lastItem(visible);

  if (current === null) {
    current = makePoint(0, null);
  }

  return sensors.map(sensor => (
    <SensorChart
      key={sensor.key}
      sensor={sensor}
      points={visible}
      value={current[sensor.key]}
      threshold={findThreshold(thresholds, sensor.metric)}
      animate={animate}
    />
  ));
}

export function ChartTooltip({ active, payload, label }: TooltipContentProps) {
  if (!active || payload.length === 0) {
    return null;
  }

  const measured = payload[0].value;

  for (const sensor of sensors) {
    if (sensor.key === payload[0].dataKey && typeof measured === "number") {
      return (
        <div className="tooltip glass">
          <small>{formatDate(Number(label))}</small>
          <strong>
            {formatNumber(measured, sensor.digits)} {sensor.unit}
          </strong>
        </div>
      );
    }
  }

  return null;
}

type ChartProps = {
  sensor: Sensor;
  points: Point[];
  value: number | null;
  threshold: Threshold | null;
  animate: boolean;
};

function SensorChart({ sensor, points, value, threshold, animate }: ChartProps) {
  const axis = { fill: "var(--muted)", fontSize: 12 };
  const duration = animationTime();
  let valueClass = "value";
  let line = null;
  let unit = null;

  if (value !== null) {
    unit = <small>{sensor.unit}</small>;
  }

  if (threshold !== null && threshold.enabled) {
    line = <ReferenceLine y={threshold.threshold} stroke="var(--red)" strokeDasharray="4 4" />;

    if (value !== null && value >= threshold.threshold) {
      valueClass = "value red";
    }
  }

  return (
    <section className="card">
      <div className="chart-header">
        <h3>{sensor.label}</h3>
        <strong className={valueClass}>
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
              width={48}
              domain={["auto", "auto"]}
              tick={axis}
              axisLine={false}
              tickLine={false}
              tickFormatter={tick => formatNumber(tick, sensor.digits)}
            />
            <Tooltip animationDuration={duration} content={ChartTooltip} />
            {line}
            <Line
              dataKey={sensor.key}
              stroke="var(--accent)"
              strokeWidth={2}
              dot={false}
              isAnimationActive={animate}
              animationDuration={duration}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </section>
  );
}
