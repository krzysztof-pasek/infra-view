import { useState } from "react";
import { Appear } from "./Appear";
import { Icon } from "./Icon";

type Props = {
  path: string;
  live: boolean;
  paused: boolean;
  max: number;
  delay: number;
  label: string | null;
  onDelay: (delay: number) => void;
  onPause: () => void;
  onPlay: () => void;
};

export function CameraPanel({ path, live, paused, max, delay, label, onDelay, onPause, onPlay }: Props) {
  const [failedPath, setFailedPath] = useState("");
  let image = <p className="camera-status">Brak obrazu z kamery</p>;

  if (path !== "" && path !== failedPath) {
    image = <img src={path} alt="Obraz z kamery termowizyjnej" onError={() => setFailedPath(path)} />;
  }

  let liveButton = null;

  if (live) {
    liveButton = (
      <button aria-pressed={delay === 0 && !paused} onClick={() => onDelay(0)}>
        Na żywo
      </button>
    );
  }

  let timeLabel = null;

  if (label !== null) {
    timeLabel = <span>{label}</span>;
  }

  let toggleLabel = "Wstrzymaj";
  let toggleClass = "icon swap primary";
  let toggle = onPause;

  if (paused) {
    toggleLabel = "Wznów";
    toggleClass = "icon swap on";
    toggle = onPlay;
  }

  return (
    <div className="camera">
      {image}

      <div className="camera-controls glass">
        {liveButton}
        <input
          type="range"
          min={0}
          max={max}
          value={max - delay}
          aria-label="Przewijanie obrazu"
          onChange={event => onDelay(max - Number(event.target.value))}
        />
        <Appear>{timeLabel}</Appear>
        <button className={toggleClass} aria-label={toggleLabel} title={toggleLabel} onClick={toggle}>
          <Icon d="M7 5h3v14H7zM14 5h3v14h-3z" />
          <Icon d="M8 5v14l11-7z" />
        </button>
      </div>
    </div>
  );
}
