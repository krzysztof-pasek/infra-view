import { useState } from "react";
import type { SubmitEvent } from "react";
import type { Metric, Threshold } from "./types";
import { sensors } from "./format";
import { Modal } from "./Modal";

export type ThresholdValues = {
  threshold: number;
  resetMargin: number;
  enabled: boolean;
};

type Props = {
  thresholds: Threshold[] | null;
  error: string;
  busy: boolean;
  onSave: (metric: Metric, values: ThresholdValues) => void;
  onClose: () => void;
};

export function ThresholdsDialog({ thresholds, error, busy, onSave, onClose }: Props) {
  let content = <p className="muted">Ładowanie…</p>;

  if (error !== "") {
    content = <p className="orange">{error}</p>;
  }

  if (thresholds !== null) {
    content = (
      <div>
        {thresholds.map(threshold => (
          <ThresholdRow
            key={threshold.metric + threshold.threshold + threshold.resetMargin + threshold.enabled}
            threshold={threshold}
            busy={busy}
            onSave={onSave}
          />
        ))}
      </div>
    );
  }

  return (
    <Modal title="Progi alarmów" onClose={onClose}>
      <p className="muted">
        Alarm włącza się, gdy pomiar przekroczy próg. Kolejny alarm tego typu pojawi się dopiero, gdy wartość spadnie
        poniżej progu o margines.
      </p>

      {content}
    </Modal>
  );
}

type RowProps = {
  threshold: Threshold;
  busy: boolean;
  onSave: (metric: Metric, values: ThresholdValues) => void;
};

function ThresholdRow({ threshold, busy, onSave }: RowProps) {
  const [value, setValue] = useState(String(threshold.threshold));
  const [margin, setMargin] = useState(String(threshold.resetMargin));
  const [enabled, setEnabled] = useState(threshold.enabled);
  let label: string = threshold.metric;

  for (const sensor of sensors) {
    if (sensor.metric === threshold.metric) {
      label = `${sensor.label} (${sensor.unit})`;
    }
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(threshold.metric, { threshold: Number(value), resetMargin: Number(margin), enabled });
  }

  return (
    <form className="threshold" onSubmit={submit}>
      <strong>{label}</strong>

      <label>
        Próg
        <input type="number" step="any" required value={value} onChange={event => setValue(event.target.value)} />
      </label>

      <label>
        Margines
        <input type="number" step="any" min={0} required value={margin} onChange={event => setMargin(event.target.value)} />
      </label>

      <label className="toggle">
        <input type="checkbox" checked={enabled} onChange={event => setEnabled(event.target.checked)} />
        Włączony
      </label>

      <button className="primary" disabled={busy}>
        Zapisz
      </button>
    </form>
  );
}
