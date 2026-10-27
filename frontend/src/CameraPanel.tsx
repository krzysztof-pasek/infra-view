import { useEffect, useState } from "react";
import { formatDelay, MAX_REPLAY } from "./format";

const RECONNECT_DELAY = 2000;
const REPLAY_STEP = 5;

function join(first: Uint8Array, second: Uint8Array) {
  const result = new Uint8Array(first.length + second.length);
  result.set(first);
  result.set(second, first.length);
  return result;
}

function findMarker(buffer: Uint8Array, marker: number, from: number) {
  for (let i = from; i < buffer.length - 1; i++) {
    if (buffer[i] === 0xff && buffer[i + 1] === marker) {
      return i;
    }
  }

  return -1;
}

export function CameraPanel({ incidentId }: { incidentId: number }) {
  const [delay, setDelay] = useState(0);
  const [frame, setFrame] = useState("");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let url = "";
    let timer = 0;

    let path = `/api/incidents/${incidentId}/live`;

    if (delay > 0) {
      path = `/api/incidents/${incidentId}/replay?delaySeconds=${delay}`;
    }

    async function read() {
      try {
        const response = await fetch(path, { signal: controller.signal });

        if (response.ok && response.body !== null) {
          const reader = response.body.getReader();
          let buffer = new Uint8Array(0);

          while (true) {
            const chunk = await reader.read();

            if (chunk.done) {
              break;
            }

            buffer = join(buffer, chunk.value);
            let start = findMarker(buffer, 0xd8, 0);
            let end = findMarker(buffer, 0xd9, start + 2);

            while (start !== -1 && end !== -1) {
              URL.revokeObjectURL(url);
              url = URL.createObjectURL(new Blob([buffer.slice(start, end + 2)], { type: "image/jpeg" }));
              setFrame(url);
              buffer = buffer.slice(end + 2);
              start = findMarker(buffer, 0xd8, 0);
              end = findMarker(buffer, 0xd9, start + 2);
            }
          }
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          console.warn("Błąd strumienia kamery", error);
        }
      }

      if (!controller.signal.aborted) {
        setFrame("");
        timer = window.setTimeout(() => setAttempt(value => value + 1), RECONNECT_DELAY);
      }
    }

    read();

    return () => {
      controller.abort();
      clearTimeout(timer);
      URL.revokeObjectURL(url);
    };
  }, [incidentId, delay, attempt]);

  let image = <p className="camera-status">Brak obrazu z kamery</p>;

  if (frame !== "") {
    image = <img src={frame} alt="Obraz z kamery termowizyjnej" />;
  }

  let position = "0:00";

  if (delay > 0) {
    position = formatDelay(delay);
  }

  return (
    <div className="camera">
      {image}

      <div className="camera-controls glass">
        <button aria-pressed={delay === 0} onClick={() => setDelay(0)}>
          Na żywo
        </button>
        <input
          type="range"
          min={0}
          max={MAX_REPLAY}
          step={REPLAY_STEP}
          value={MAX_REPLAY - delay}
          aria-label="Przewijanie obrazu"
          onChange={event => setDelay(MAX_REPLAY - Number(event.target.value))}
        />
        <span>{position}</span>
      </div>
    </div>
  );
}
