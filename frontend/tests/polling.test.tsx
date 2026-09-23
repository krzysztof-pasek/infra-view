import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { usePolling } from "../src/usePolling";

const ONE_SECOND = 1000;
const ONE_MINUTE = 60_000;

const hangingRequest = () => new Promise<number>(() => {});

afterEach(() => {
  vi.useRealTimers();
});

it("pobiera dane", async () => {
  const load = async () => 1;
  const { result } = renderHook(() => usePolling(load, ONE_MINUTE));

  await waitFor(() => expect(result.current.data).toBe(1));
  expect(result.current.error).toBeNull();
});

it("zachowuje dane po błędzie", async () => {
  const load = vi.fn()
    .mockResolvedValueOnce(1)
    .mockRejectedValueOnce(new Error("Brak sieci"));
  const { result } = renderHook(() => usePolling(load, ONE_MINUTE));
  await waitFor(() => expect(result.current.data).toBe(1));

  act(() => result.current.refresh());

  await waitFor(() => expect(result.current.error).toBe("Brak sieci"));
  expect(result.current.data).toBe(1);
});

it("stara odpowiedź nie nadpisuje nowszej zmiany", async () => {
  let finishOldRequest!: (value: number) => void;
  const load = vi.fn()
    .mockImplementationOnce(() => new Promise(resolve => { finishOldRequest = resolve; }))
    .mockImplementation(hangingRequest);
  const { result } = renderHook(() => usePolling(load, ONE_MINUTE));

  act(() => result.current.update(() => 2));
  await act(async () => finishOldRequest(1));

  expect(result.current.data).toBe(2);
});

it("anuluje żądanie po zamknięciu komponentu", () => {
  let signal!: AbortSignal;
  const load = (currentSignal: AbortSignal) => {
    signal = currentSignal;
    return hangingRequest();
  };
  const { unmount } = renderHook(() => usePolling(load, ONE_MINUTE));

  unmount();

  expect(signal.aborted).toBe(true);
});

it("nie wysyła nowego żądania, dopóki poprzednie trwa", async () => {
  vi.useFakeTimers();
  const load = vi.fn(hangingRequest);
  renderHook(() => usePolling(load, ONE_SECOND));

  await act(() => vi.advanceTimersByTimeAsync(5 * ONE_SECOND));

  expect(load).toHaveBeenCalledTimes(1);
});