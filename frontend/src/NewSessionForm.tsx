import { useState } from "react";
import type { FormEvent } from "react";
import type { NewIncident } from "./types";

type Props = {
  busy: boolean;
  onCreate: (input: NewIncident) => Promise<boolean>;
};

export function NewSessionForm({ busy, onCreate }: Props) {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!code.trim() || !name.trim() || busy) {
      return;
    }

    const success = await onCreate({
      code: code.trim(),
      firefighterName: name.trim(),
      description: null,
      location: null,
      startedAt: new Date().toISOString(),
      endedAt: null,
      status: "IN_PROGRESS",
    });

    if (success) {
      setCode("");
      setName("");
    }
  }

  return (
    <details className="panel">
      <summary>Nowa sesja</summary>

      <form className="form" onSubmit={submit}>
        <label>
          Kod sesji

          <input
            required
            maxLength={20}
            value={code}
            disabled={busy}
            onChange={event => setCode(event.target.value)}
          />
        </label>

        <label>
          Imię i nazwisko

          <input
            required
            maxLength={100}
            value={name}
            disabled={busy}
            onChange={event => setName(event.target.value)}
          />
        </label>

        <button
          type="submit"
          disabled={busy || !code.trim() || !name.trim()}
        >
          {busy ? "Zapisywanie..." : "Utwórz sesję"}
        </button>
      </form>
    </details>
  );
}