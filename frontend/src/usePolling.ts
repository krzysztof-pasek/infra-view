import { useEffect, useState } from "react";
import { api, errorMessage } from "./api";

type Snapshot<T> = {
  path: string;
  data: T | null;
  error: string;
  updatedAt: number;
  changedAt: number;
};

export function usePolling<T>(path: string | null, interval: number) {
  const [snapshot, setSnapshot] = useState<Snapshot<T> | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (path === null) {
      return;
    }

    let active = true;
    let timer = 0;
    let lastText = "";
    let changedAt = 0;

    async function load(currentPath: string) {
      try {
        const data = await api<T>(currentPath);
        const text = JSON.stringify(data);

        if (text !== lastText) {
          lastText = text;
          changedAt = Date.now();
        }

        if (active) {
          setSnapshot({ path: currentPath, data, error: "", updatedAt: Date.now(), changedAt });
        }
      } catch (error) {
        const message = errorMessage(error, "Nie udało się pobrać danych");

        if (active) {
          setSnapshot(previous => {
            if (previous !== null && previous.path === currentPath) {
              return { ...previous, error: message };
            }

            return { path: currentPath, data: null, error: message, updatedAt: 0, changedAt: 0 };
          });
        }
      }

      if (active) {
        timer = window.setTimeout(() => load(currentPath), interval);
      }
    }

    load(path);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [path, interval, version]);

  function refresh() {
    setVersion(value => value + 1);
  }

  if (snapshot === null || snapshot.path !== path) {
    return { data: null, error: "", updatedAt: 0, changedAt: 0, refresh };
  }

  return { ...snapshot, refresh };
}
