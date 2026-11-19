import { useEffect, useRef, useState } from "react";
import type { SubmitEvent } from "react";
import { Modal } from "./Modal";

const MAX_CODE = 20;

type Props = {
  code: string;
  busy: boolean;
  onSave: (code: string) => void;
  onClose: () => void;
};

export function RenameDialog({ code, busy, onSave, onClose }: Props) {
  const [newCode, setNewCode] = useState(code);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (input.current !== null) {
      input.current.focus();
      input.current.select();
    }
  }, []);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(newCode.trim());
  }

  const save = (
    <button className="primary" form="rename" disabled={busy || newCode.trim() === ""}>
      Zapisz
    </button>
  );

  return (
    <Modal title="Zmień nazwę sesji" action={save} alert onClose={onClose}>
      <form id="rename" onSubmit={submit}>
        <input
          ref={input}
          value={newCode}
          maxLength={MAX_CODE}
          aria-label="Nazwa sesji"
          onChange={event => setNewCode(event.target.value)}
        />
      </form>
    </Modal>
  );
}
