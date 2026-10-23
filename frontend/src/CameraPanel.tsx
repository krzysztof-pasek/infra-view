import { useEffect, useState } from "react";

const RECONNECT_DELAY = 2000;

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
  const [frame, setFrame] = useState("");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let url = "";
    let timer = 0;

    const path = `/api/incidents/${incidentId}/live`;

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
  }, [incidentId, attempt]);

  let image = <p className="camera-status">Brak obrazu z kamery</p>;

  if (frame !== "") {
    image = <img src={frame} alt="Obraz z kamery termowizyjnej" />;
  }

  return (
    <div className="camera">
      {image}
    </div>
  );
}
