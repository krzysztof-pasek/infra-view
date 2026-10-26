import { useState } from "react";
import type { Incident } from "./types";
import { incidentName } from "./data";
import { formatDate, formatDay } from "./format";

type Props = {
  incidents: Incident[];
  onOpen: (id: number) => void;
  onClose: () => void;
};

function matches(incident: Incident, query: string, from: string, to: string, status: string) {
  const text = (incidentName(incident) + " " + incident.code).toLowerCase();
  const day = formatDay(incident.startedAt);

  if (query !== "" && !text.includes(query)) {
    return false;
  }

  if (from !== "" && day < from) {
    return false;
  }

  if (to !== "" && day > to) {
    return false;
  }

  if (status !== "" && incident.status !== status) {
    return false;
  }

  return true;
}

function endOf(incident: Incident) {
  if (incident.endedAt === null) {
    return "w trakcie";
  }

  return formatDate(incident.endedAt);
}

export function ArchiveDialog({ incidents, onOpen, onClose }: Props) {
  const [query, setQuery] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [status, setStatus] = useState("");

  const found: Incident[] = [];

  for (const incident of incidents) {
    if (matches(incident, query.trim().toLowerCase(), from, to, status)) {
      found.push(incident);
    }
  }

  found.sort((a, b) => Date.parse(b.startedAt) - Date.parse(a.startedAt));

  let content = <p className="muted">Brak sesji dla wybranych filtrów</p>;

  if (found.length > 0) {
    content = (
      <div className="scroll">
        <table>
          <thead>
            <tr>
              <th>Strażak</th>
              <th>Kod</th>
              <th>Początek</th>
              <th>Koniec</th>
              <th />
            </tr>
          </thead>

          <tbody>
            {found.map(incident => (
              <tr key={incident.id}>
                <td>{incidentName(incident)}</td>
                <td>{incident.code}</td>
                <td>{formatDate(incident.startedAt)}</td>
                <td>{endOf(incident)}</td>
                <td>
                  <button onClick={() => onOpen(incident.id)}>Otwórz</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal glass" role="dialog" aria-label="Archiwum sesji" onClick={event => event.stopPropagation()}>
        <div className="modal-header">
          <h2>Archiwum sesji</h2>
          <button onClick={onClose}>Zamknij</button>
        </div>

        <div className="filters">
          <label>
            Szukaj
            <input value={query} placeholder="Nazwa lub kod" onChange={event => setQuery(event.target.value)} />
          </label>
          <label>
            Od
            <input type="date" value={from} onChange={event => setFrom(event.target.value)} />
          </label>
          <label>
            Do
            <input type="date" value={to} onChange={event => setTo(event.target.value)} />
          </label>
          <label>
            Status
            <select value={status} onChange={event => setStatus(event.target.value)}>
              <option value="">Wszystkie</option>
              <option value="IN_PROGRESS">W trakcie</option>
              <option value="RESOLVED">Zakończone</option>
            </select>
          </label>
        </div>

        {content}
      </div>
    </div>
  );
}
