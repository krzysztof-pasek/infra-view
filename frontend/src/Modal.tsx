import { useEffect, useRef, useState } from "react";
import type { MouseEvent, ReactNode } from "react";

type Props = {
  title: string;
  action?: ReactNode;
  alert?: boolean;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ title, action, alert, onClose, children }: Props) {
  const [previousFocus] = useState(document.activeElement);
  const dialog = useRef<HTMLDivElement>(null);

  useEffect(() => {
    return () => {
      setTimeout(() => {
        if (document.activeElement === document.body && previousFocus instanceof HTMLElement) {
          previousFocus.focus();
        }
      });
    };
  }, [previousFocus]);

  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      let last = null;

      for (const other of document.querySelectorAll('[role="dialog"]')) {
        if (other.closest(".closing") === null) {
          last = other;
        }
      }

      if (event.key === "Escape" && last === dialog.current) {
        onClose();
      }
    }

    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  function closeOnOverlay(event: MouseEvent) {
    event.preventDefault();
    onClose();
  }

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
  let footer = null;

  if (alert) {
    className = "modal glass alert";
    header = null;
    footer = buttons;
  }

  return (
    <div className="overlay" onMouseDown={closeOnOverlay}>
      <div
        ref={dialog}
        className={className}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={event => event.stopPropagation()}
      >
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
