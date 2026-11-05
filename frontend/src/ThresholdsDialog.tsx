import { useRef, useState } from "react";
import type { SubmitEvent } from "react";
import type { Threshold } from "./types";
import { sensors } from "./format";
import { Modal } from "./Modal";
import { Confirm } from "./Confirm";
import { ErrorLine } from "./ErrorLine";
import { Loading } from "./Loading";
import { Appear } from "./Appear";

type Props = {
  thresholds: Threshold[] | null;
  error: string;
  busy: boolean;
  onSave: (thresholds: Threshold[]) => void;
  onClose: () => void;
};

export function ThresholdsDialog({ thresholds, error, busy, onSave, onClose }: Props) {
  const [initial, setInitial] = useState<Threshold[] | null>(null);
  const [discard, setDiscard] = useState(false);
  const form = useRef<HTMLFormElement>(null);
  let content = <Loading />;

  if (initial === null && thresholds !== null) {
    setInitial(thresholds);
  }

  function changes() {
    const changed: Threshold[] = [];

    if (initial === null || form.current === null) {
      return changed;
    }

    const data = new FormData(form.current);

    for (const threshold of initial) {
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

    return changed;
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(changes());
  }

  function close() {
    if (changes().length > 0) {
      setDiscard(true);
      return;
    }

    onClose();
  }

  if (error !== "") {
    content = <ErrorLine error={error} />;
  }

  if (initial !== null) {
    content = (
      <form id="thresholds" ref={form} onSubmit={submit}>
        {initial.map(threshold => (
          <ThresholdRow key={threshold.metric} threshold={threshold} />
        ))}
      </form>
    );
  }

  const save = (
    <button className="primary" form="thresholds" disabled={busy || initial === null}>
      Zapisz
    </button>
  );

  let question = null;

  if (discard) {
    question = (
      <Confirm
        title="Porzucić zmiany?"
        text="Zmienione progi nie zostały zapisane."
        button="Porzuć"
        onYes={onClose}
        onNo={() => setDiscard(false)}
      />
    );
  }

  return (
    <>
      <Modal title="Progi alarmów" action={save} onClose={close}>
        <p className="muted">
          Alarm włącza się, gdy pomiar przekroczy próg. Kolejny alarm tego typu pojawi się dopiero, gdy wartość spadnie
          poniżej progu o margines.
        </p>

        {content}
      </Modal>

      <Appear>{question}</Appear>
    </>
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
