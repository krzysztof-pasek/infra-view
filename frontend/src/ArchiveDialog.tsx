import { useState } from "react";
import type { Incident } from "./types";
import { formatDate, formatDay } from "./format";
import { Modal } from "./Modal";

type Props = {
  incidents: Incident[];
  onOpen: (id: number) => void;
  onClose: () => void;
};

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

  function matches(incident: Incident) {
    const day = formatDay(incident.startedAt);

    if (!incident.code.toLowerCase().includes(query.trim().toLowerCase())) {
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

  const found: Incident[] = [];

  for (const incident of incidents) {
    if (matches(incident)) {
      found.push(incident);
    }
  }

  found.sort((first, second) => Date.parse(second.startedAt) - Date.parse(first.startedAt));

  let content = <p className="muted">Brak sesji dla wybranych filtrów</p>;

  if (found.length > 0) {
    content = (
      <div className="scroll">
        <table>
          <thead>
            <tr>
              <th>Sesja</th>
              <th>Początek</th>
              <th>Koniec</th>
              <th />
            </tr>
          </thead>

          <tbody>
            {found.map(incident => (
              <tr key={incident.id}>
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
    <Modal title="Archiwum sesji" onClose={onClose}>
      <div className="filters">
        <label>
          Szukaj
          <input value={query} placeholder="Nazwa sesji" onChange={event => setQuery(event.target.value)} />
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
    </Modal>
  );
}
