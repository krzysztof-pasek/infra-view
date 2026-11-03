import { useEffect, useState } from "react";
import { formatDelay, MAX_REPLAY } from "./format";

const RECONNECT_DELAY = 2000;
const PAUSE_ICON = "M7 5h3v14H7zM14 5h3v14h-3z";
const PLAY_ICON = "M8 5v14l11-7z";

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

type Props = {
  incidentId: number;
  delay: number;
  paused: boolean;
  onDelay: (delay: number) => void;
  onPause: () => void;
  onPlay: () => void;
};

export function CameraPanel({ incidentId, delay, paused, onDelay, onPause, onPlay }: Props) {
  const [frame, setFrame] = useState("");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (paused) {
      return;
    }

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

            while (true) {
              const start = findMarker(buffer, 0xd8, 0);
              const end = findMarker(buffer, 0xd9, start + 2);

              if (start === -1 || end === -1) {
                break;
              }

              URL.revokeObjectURL(url);
              url = URL.createObjectURL(new Blob([buffer.slice(start, end + 2)], { type: "image/jpeg" }));
              setFrame(url);
              buffer = buffer.slice(end + 2);
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
    };
  }, [incidentId, delay, attempt, paused]);

  useEffect(() => {
    return () => URL.revokeObjectURL(frame);
  }, [frame]);

  let image = <p className="camera-status">Brak obrazu z kamery</p>;

  if (frame !== "") {
    image = <img src={frame} alt="Obraz z kamery termowizyjnej" />;
  }

  let toggleLabel = "Wstrzymaj";
  let toggleIcon = PAUSE_ICON;
  let toggleClass = "icon primary";
  let toggle = onPause;

  if (paused) {
    toggleLabel = "Wznów";
    toggleIcon = PLAY_ICON;
    toggleClass = "icon";
    toggle = onPlay;
  }

  return (
    <div className="camera">
      {image}

      <div className="camera-controls glass">
        <button aria-pressed={delay === 0 && !paused} onClick={() => onDelay(0)}>
          Na żywo
        </button>
        <input
          type="range"
          min={0}
          max={MAX_REPLAY}
          value={MAX_REPLAY - delay}
          aria-label="Przewijanie obrazu"
          onChange={event => onDelay(MAX_REPLAY - Number(event.target.value))}
        />
        <span>{formatDelay(delay)}</span>
        <button className={toggleClass} aria-label={toggleLabel} title={toggleLabel} onClick={toggle}>
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d={toggleIcon} />
          </svg>
        </button>
      </div>
    </div>
  );
}
