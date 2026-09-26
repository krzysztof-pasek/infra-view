import type { Incident } from "./types";

type Props = {
  incidents: Incident[];
  selectedId: number | null;
  onSelect: (id: number) => void;
};

export function SessionList({ incidents, selectedId, onSelect }: Props) {
  return (
    <aside className="panel">
      <h2>Sesje strażaków</h2>

      {incidents.length === 0 && (
        <p className="muted">Brak sesji</p>
      )}

      <ul className="session-list">
        {incidents.map(incident => (
          <li key={incident.id}>
            <button
              className="session"
              aria-pressed={selectedId === incident.id}
              onClick={() => onSelect(incident.id)}
            >
              <strong>{incident.firefighterName || incident.code}</strong>
              <small>{incident.code}</small>
              <span>
                {incident.status === "IN_PROGRESS" ? "W trakcie" : "Zakończona"}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </aside>
  );
}