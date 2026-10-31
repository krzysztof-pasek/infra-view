import type { ReactNode } from "react";

type Props = {
  title: string;
  action?: ReactNode;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ title, action, onClose, children }: Props) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal glass" role="dialog" aria-label={title} onClick={event => event.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <div className="toolbar">
            {action}
            <button onClick={onClose}>Zamknij</button>
          </div>
        </div>

        {children}
      </div>
    </div>
  );
}
