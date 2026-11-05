import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { MouseEvent } from "react";
import type { Incident, Telemetry } from "./types";
import { formatAge, formatDate, motionStates } from "./format";
import { Appear } from "./Appear";
import { Icon } from "./Icon";

type Props = {
  incident: Incident | undefined;
  latest: Telemetry | undefined;
  online: boolean;
  now: number;
  busy: boolean;
  onEnd: () => void;
  onRename: () => void;
  onClose: () => void;
};

export function SessionBar({ incident, latest, online, now, busy, onEnd, onRename, onClose }: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuTop, setMenuTop] = useState(0);
  const [menuRight, setMenuRight] = useState(0);
  const button = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    function close() {
      setMenuOpen(false);
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && button.current !== null) {
        setMenuOpen(false);
        button.current.focus();
      }
    }

    document.addEventListener("click", close);
    document.addEventListener("keydown", closeOnEscape);
    window.addEventListener("scroll", close);

    return () => {
      document.removeEventListener("click", close);
      document.removeEventListener("keydown", closeOnEscape);
      window.removeEventListener("scroll", close);
    };
  }, [menuOpen]);

  if (incident === undefined) {
    return <div className="session-bar" />;
  }

  function toggleMenu(event: MouseEvent<HTMLButtonElement>) {
    const rect = event.currentTarget.getBoundingClientRect();
    event.stopPropagation();
    setMenuTop(rect.bottom + 8);
    setMenuRight(document.documentElement.clientWidth - rect.right);
    setMenuOpen(!menuOpen);
  }

  function choose(action: () => void) {
    if (button.current !== null) {
      button.current.focus();
    }

    action();
  }

  let status = "Zakończona";
  let statusColor = "muted";
  let endItem = null;

  if (incident.status === "IN_PROGRESS") {
    status = "Brak danych z kasku";
    statusColor = "orange";
    endItem = (
      <button className="red" role="menuitem" disabled={busy} onClick={() => choose(onEnd)}>
        Zakończ sesję
      </button>
    );

    if (online) {
      status = "Na żywo";
      statusColor = "";
    }

    if (!online && latest !== undefined) {
      status = status + " · " + formatAge(now - Date.parse(latest.recordedAt));
    }
  }

  let motion = "—";
  let motionColor = "";

  if (latest !== undefined && latest.motionState !== null) {
    motion = motionStates[latest.motionState].label;
    motionColor = motionStates[latest.motionState].color;
  }

  let menu = null;

  if (menuOpen) {
    menu = (
      <div className="menu glass" role="menu" style={{ top: menuTop, right: menuRight }}>
        <button role="menuitem" onClick={() => choose(onRename)}>
          Zmień nazwę
        </button>
        {endItem}
        <button role="menuitem" onClick={() => choose(onClose)}>
          Zamknij
        </button>
      </div>
    );
  }

  return (
    <div className="session-bar">
      <div>
        <h2 title={incident.code}>{incident.code}</h2>
        <small>start {formatDate(incident.startedAt)}</small>
      </div>

      <span className={"status " + statusColor}>
        <span className="dot" />
        {status}
      </span>

      <span className={"status " + motionColor}>Ruch: {motion}</span>

      <button
        ref={button}
        className="icon"
        aria-label="Opcje sesji"
        title="Opcje sesji"
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        onClick={toggleMenu}
      >
        <Icon d="M5 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm7 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm7 0a2 2 0 1 0 0 4 2 2 0 0 0 0-4z" />
      </button>

      {createPortal(<Appear>{menu}</Appear>, document.body)}
    </div>
  );
}
