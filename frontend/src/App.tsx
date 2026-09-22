import { useState } from "react";
import { SessionList } from "./SessionList";
import { SessionDetails } from "./SessionDetails";
import type { Incident } from "./types";
import { useTheme } from "./useTheme";

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const incidents: Incident[] = [];
  const selected = incidents.find(incident => incident.id === selectedId);

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1>RescueVision</h1>
          <p className="muted">Panel monitoringu</p>
        </div>

        <button onClick={toggleTheme}>
          {theme === "dark" ? "Jasny motyw" : "Ciemny motyw"}
        </button>
      </header>

      <main className="layout">
        <SessionList
          incidents={incidents}
          selectedId={selectedId}
          onSelect={setSelectedId}
        />

        <section className="content">
          <div className="panel">
            <h2>{selected?.firefighterName || "Wybierz sesję"}</h2>
          </div>

          {selected && (
            <SessionDetails
              key={selected.id}
              incident={selected}
            />
          )}
        </section>
      </main>
    </div>
  );
}