import type { Recording } from "./types";
import { formatDate, formatNumber } from "./format";

export function RecordingList({ recordings }: { recordings: Recording[] }) {
  return (
    <section className="panel">
      <h2>Nagrania</h2>

      {recordings.length === 0 ? (
        <p className="muted">Brak opisów nagrań</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rozpoczęcie</th>
                <th>Czas</th>
                <th>Rozmiar</th>
                <th>Zakończenie</th>
              </tr>
            </thead>

            <tbody>
              {recordings.map(recording => (
                <tr key={recording.id}>
                  <td>{formatDate(recording.startedAt)}</td>

                  <td>
                    {formatNumber(recording.durationSec, 0)} s
                  </td>

                  <td>
                    {formatNumber(
                      recording.fileSizeBytes == null
                        ? null
                        : recording.fileSizeBytes / 1024 / 1024
                    )}{" "}
                    MiB
                  </td>

                  <td>{formatDate(recording.endedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}