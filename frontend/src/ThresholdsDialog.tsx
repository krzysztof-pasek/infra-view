import { useState } from "react";
import type { ReactNode, SubmitEvent } from "react";
import type { Threshold } from "./types";
import { sensors } from "./format";
import { Modal } from "./Modal";

type Props = {
  thresholds: Threshold[] | null;
  error: string;
  busy: boolean;
  onSave: (thresholds: Threshold[]) => void;
  onClose: () => void;
};

export function ThresholdsDialog({ thresholds, error, busy, onSave, onClose }: Props) {
  const [opened, setOpened] = useState<Threshold[] | null>(null);
  let content: ReactNode = <p className="muted">Ładowanie…</p>;

  if (opened === null && thresholds !== null) {
    setOpened(thresholds);
  }

  if (error !== "") {
    content = <p className="orange">{error}</p>;
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (opened === null) {
      return;
    }

    const data = new FormData(event.currentTarget);
    const changed: Threshold[] = [];

    for (const threshold of opened) {
      const value = {
        metric: threshold.metric,
        threshold: Number(data.get(threshold.metric + "-threshold")),
        resetMargin: Number(data.get(threshold.metric + "-margin")),
        enabled: data.get(threshold.metric + "-enabled") !== null,
      };

      if (value.threshold !== threshold.threshold || value.resetMargin !== threshold.resetMargin || value.enabled !== threshold.enabled) {
        changed.push(value);
      }
    }

    onSave(changed);
  }

  if (opened !== null) {
    content = (
      <form id="thresholds" onSubmit={submit}>
        {opened.map(threshold => (
          <ThresholdRow key={threshold.metric} threshold={threshold} />
        ))}
      </form>
    );
  }

  const save = (
    <button className="primary" form="thresholds" disabled={busy || opened === null}>
      Zapisz
    </button>
  );

  return (
    <Modal title="Progi alarmów" action={save} onClose={onClose}>
      <p className="muted">
        Alarm włącza się, gdy pomiar przekroczy próg. Kolejny alarm tego typu pojawi się dopiero, gdy wartość spadnie
        poniżej progu o margines.
      </p>

      {content}
    </Modal>
  );
}

function ThresholdRow({ threshold }: { threshold: Threshold }) {
  let label: string = threshold.metric;

  for (const sensor of sensors) {
    if (sensor.metric === threshold.metric) {
      label = `${sensor.label} (${sensor.unit})`;
    }
  }

  return (
    <div className="threshold">
      <strong>{label}</strong>

      <label>
        Próg
        <input name={threshold.metric + "-threshold"} type="number" step="any" required defaultValue={threshold.threshold} />
      </label>

      <label>
        Margines
        <input name={threshold.metric + "-margin"} type="number" step="any" min={0} required defaultValue={threshold.resetMargin} />
      </label>

      <label className="toggle">
        <input name={threshold.metric + "-enabled"} type="checkbox" defaultChecked={threshold.enabled} />
        Włączony
      </label>
    </div>
  );
}
