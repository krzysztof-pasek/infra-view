import type { Recording } from "./types";
import { formatDate, formatNumber } from "./format";

const BYTES_IN_MB = 1000000;

function endOf(recording: Recording) {
  if (recording.endedAt === null) {
    return "trwa";
  }

  return formatDate(recording.endedAt);
}

function durationOf(recording: Recording) {
  if (recording.durationSec === null) {
    return "—";
  }

  return formatNumber(recording.durationSec, 0) + " s";
}

function sizeOf(recording: Recording) {
  if (recording.fileSizeBytes === null) {
    return "—";
  }

  return formatNumber(recording.fileSizeBytes / BYTES_IN_MB, 1) + " MB";
}

export function RecordingList({ recordings }: { recordings: Recording[] }) {
  let content = <p className="muted">Brak nagrań</p>;

  if (recordings.length > 0) {
    content = (
      <div className="scroll">
        <table>
          <thead>
            <tr>
              <th>Początek</th>
              <th>Koniec</th>
              <th>Długość</th>
              <th>Rozmiar</th>
            </tr>
          </thead>

          <tbody>
            {recordings.map(recording => (
              <tr key={recording.id}>
                <td>{formatDate(recording.startedAt)}</td>
                <td>{endOf(recording)}</td>
                <td>{durationOf(recording)}</td>
                <td>{sizeOf(recording)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <section className="card">
      <h3>Nagrania</h3>
      {content}
    </section>
  );
}
