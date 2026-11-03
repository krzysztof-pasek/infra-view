import { useEffect } from "react";
import type { ReactNode } from "react";

type Props = {
  title: string;
  action?: ReactNode;
  alert?: boolean;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ title, action, alert, onClose, children }: Props) {
  useEffect(() => {
    function close(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", close);
    return () => document.removeEventListener("keydown", close);
  }, [onClose]);

  const buttons = (
    <div className="toolbar">
      {action}
      <button autoFocus onClick={onClose}>
        Zamknij
      </button>
    </div>
  );

  let className = "modal glass";
  let header: ReactNode = buttons;
  let footer: ReactNode = null;

  if (alert) {
    className = "modal glass alert";
    header = null;
    footer = buttons;
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className={className} role="dialog" aria-label={title} onClick={event => event.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          {header}
        </div>

        {children}
        {footer}
      </div>
    </div>
  );
}
