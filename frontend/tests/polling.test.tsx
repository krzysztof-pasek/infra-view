import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { usePolling } from "../src/usePolling";

const ONE_SECOND = 1000;
const ONE_MINUTE = 60_000;

afterEach(() => {
  vi.useRealTimers();
});

it("pobiera dane", async () => {
  vi.stubGlobal("fetch", vi.fn(async () => Response.json(1)));
  const { result } = renderHook(() => usePolling<number>("/test", ONE_MINUTE));

  await waitFor(() => expect(result.current.data).toBe(1));
  expect(result.current.error).toBe("");
});

it("nie pyta serwera, gdy nie ma adresu", () => {
  const fetchMock = vi.fn(async () => Response.json(1));
  vi.stubGlobal("fetch", fetchMock);
  const { result } = renderHook(() => usePolling<number>(null, ONE_MINUTE));

  expect(result.current.data).toBeNull();
  expect(fetchMock).not.toHaveBeenCalled();
});

it("zachowuje dane po błędzie i pokazuje polski komunikat", async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(Response.json(1))
    .mockResolvedValueOnce(new Response(null, { status: 500 }));
  vi.stubGlobal("fetch", fetchMock);
  const { result } = renderHook(() => usePolling<number>("/test", ONE_MINUTE));
  await waitFor(() => expect(result.current.data).toBe(1));

  act(() => result.current.refresh());

  await waitFor(() => expect(result.current.error).toBe("Serwer odrzucił żądanie (błąd 500)"));
  expect(result.current.data).toBe(1);
});

it("nie pokazuje danych poprzedniego adresu", async () => {
  vi.stubGlobal("fetch", vi.fn(async (url: string) => Response.json(url)));
  const { result, rerender } = renderHook(({ path }) => usePolling<string>(path, ONE_MINUTE), {
    initialProps: { path: "/a" },
  });
  await waitFor(() => expect(result.current.data).toBe("/api/a"));

  rerender({ path: "/b" });

  expect(result.current.data).toBeNull();
  await waitFor(() => expect(result.current.data).toBe("/api/b"));
});

it("nie wysyła nowego żądania, dopóki poprzednie trwa", async () => {
  vi.useFakeTimers();
  const fetchMock = vi.fn(() => new Promise<Response>(() => {}));
  vi.stubGlobal("fetch", fetchMock);
  renderHook(() => usePolling("/test", ONE_SECOND));

  await act(() => vi.advanceTimersByTimeAsync(5 * ONE_SECOND));

  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("po zerwaniu połączenia wraca do pobierania danych", async () => {
  vi.useFakeTimers();
  const fetchMock = vi.fn()
    .mockRejectedValueOnce(new TypeError("Failed to fetch"))
    .mockImplementation(async () => Response.json(1));
  vi.stubGlobal("fetch", fetchMock);
  const { result } = renderHook(() => usePolling<number>("/test", ONE_SECOND));

  await act(() => vi.advanceTimersByTimeAsync(10));
  expect(result.current.error).toBe("Brak połączenia z serwerem");

  await act(() => vi.advanceTimersByTimeAsync(ONE_SECOND));
  expect(result.current.data).toBe(1);
  expect(result.current.error).toBe("");
});
