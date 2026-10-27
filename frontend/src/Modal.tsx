import type { ReactNode } from "react";

type Props = {
  title: string;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ title, onClose, children }: Props) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal glass" role="dialog" aria-label={title} onClick={event => event.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button onClick={onClose}>Zamknij</button>
        </div>

        {children}
      </div>
    </div>
  );
}
