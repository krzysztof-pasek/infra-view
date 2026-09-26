import { useEffect, useRef, useState } from "react";

type Snapshot<T> = {
  data: T | null;
  error: string | null;
  updatedAt: number | null;
};

export function usePolling<T>(
  load: (signal: AbortSignal) => Promise<T>,
  interval: number
) {
  const [snapshot, setSnapshot] = useState<Snapshot<T>>({
    data: null,
    error: null,
    updatedAt: null,
  });

  const [revision, setRevision] = useState(0);
  const activeController = useRef<AbortController | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    activeController.current = controller;

    let timer: ReturnType<typeof setTimeout>;

    async function poll() {
      try {
        const data = await load(controller.signal);

        if (!controller.signal.aborted) {
          setSnapshot({
            data,
            error: null,
            updatedAt: Date.now(),
          });
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setSnapshot(previous => ({
            ...previous,
            error:
              error instanceof Error
                ? error.message
                : "Nie udało się pobrać danych",
          }));
        }
      } finally {
        if (!controller.signal.aborted) {
          timer = setTimeout(poll, interval);
        }
      }
    }

    void poll();

    return () => {
      controller.abort();
      clearTimeout(timer);

      if (activeController.current === controller) {
        activeController.current = null;
      }
    };
  }, [load, interval, revision]);

  function refresh() {
    activeController.current?.abort();
    setRevision(value => value + 1);
  }

  function update(change: (previous: T | null) => T) {
    refresh();

    setSnapshot(previous => ({
      ...previous,
      data: change(previous.data),
      error: null,
    }));
  }

  return {
    ...snapshot,
    refresh,
    update,
  };
}