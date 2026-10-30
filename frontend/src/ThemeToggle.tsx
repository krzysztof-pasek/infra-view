import { useState } from "react";
import { Icon } from "./Icon";

export function ThemeToggle() {
  const [theme, setTheme] = useState(document.documentElement.dataset.theme);
  let label = "Włącz ciemny motyw";
  let className = "icon swap theme-toggle";
  let next = "dark";

  if (theme === "dark") {
    label = "Włącz jasny motyw";
    className = "icon swap theme-toggle on";
    next = "light";
  }

  function toggle() {
    document.documentElement.dataset.theme = next;
    localStorage.setItem("theme", next);
    setTheme(next);
  }

  return (
    <button className={className} aria-label={label} title={label} onClick={toggle}>
      <Icon d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
      <Icon d="M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </button>
  );
}
