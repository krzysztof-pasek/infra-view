import { useTheme } from "./useTheme";

export default function App() {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="app">
      <header className="topbar">
        <div><h1>INFRA VIEW</h1><p className="muted">Panel monitoringu</p></div>
        <button onClick={toggleTheme}>
          {theme === "dark" ? "Włącz jasny motyw" : "Włącz ciemny motyw"}
        </button>
      </header>
      <main className="layout">
        <aside className="panel"><h2>Sesje strażaków</h2></aside>
        <section className="content"><div className="panel">Wybierz sesję</div></section>
      </main>
    </div>
  );
}