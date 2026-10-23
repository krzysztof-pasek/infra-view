import { useState } from "react";

export function useTheme() {
  const [theme, setTheme] = useState(document.documentElement.dataset.theme);

  function toggleTheme() {
    let next = "dark";

    if (theme === "dark") {
      next = "light";
    }

    document.documentElement.dataset.theme = next;

    try {
      localStorage.setItem("theme", next);
    } catch {
      console.warn("Nie udało się zapisać motywu");
    }

    setTheme(next);
  }

  return { theme, toggleTheme };
}
