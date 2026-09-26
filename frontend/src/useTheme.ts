import { useEffect, useState } from "react";

type Theme = "light" | "dark";

function readSavedTheme() {
  try {
    return localStorage.getItem("infra-view-theme");
  } catch {
    return null;
  }
}

function saveTheme(theme: Theme) {
  try {
    localStorage.setItem("infra-view-theme", theme);
    return true;
  } catch {
    return false;
  }
}

function getInitialTheme(): Theme {
  const savedTheme = readSavedTheme();

  if (savedTheme === "light" || savedTheme === "dark") {
    return savedTheme;
  }

  if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
    return "dark";
  }

  return "light";
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    saveTheme(theme);
  }, [theme]);

  function toggleTheme() {
    setTheme(current => current === "light" ? "dark" : "light");
  }

  return { theme, toggleTheme };
}