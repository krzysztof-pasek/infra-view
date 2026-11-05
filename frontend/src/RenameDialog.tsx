import { useEffect, useRef, useState } from "react";
import type { SubmitEvent } from "react";
import { Modal } from "./Modal";

const MAX_NAME = 20;

type Props = {
  code: string;
  busy: boolean;
  onSave: (code: string) => void;
  onClose: () => void;
};

export function RenameDialog({ code, busy, onSave, onClose }: Props) {
  const [name, setName] = useState(code);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (input.current !== null) {
      input.current.focus();
      input.current.select();
    }
  }, []);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(name.trim());
  }

  const save = (
    <button className="primary" form="rename" disabled={busy || name.trim() === ""}>
      Zapisz
    </button>
  );

  return (
    <Modal title="Zmień nazwę sesji" action={save} alert onClose={onClose}>
      <form id="rename" onSubmit={submit}>
        <input
          ref={input}
          value={name}
          maxLength={MAX_NAME}
          aria-label="Nazwa sesji"
          onChange={event => setName(event.target.value)}
        />
      </form>
    </Modal>
  );
}
