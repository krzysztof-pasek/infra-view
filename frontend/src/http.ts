export async function request(
  path: string,
  options: RequestInit = {}
): Promise<unknown> {
  const response = await fetch(`/api${path}`, {
    ...options,
    cache: "no-store",
    signal: AbortSignal.any([
      ...(options.signal ? [options.signal] : []),
      AbortSignal.timeout(8000),
    ]),
  });

  if (!response.ok) {
    throw new Error(`Błąd API: HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();

  if (!text.trim()) {
    return null;
  }

  if (!response.headers.get("content-type")?.includes("application/json")) {
    throw new Error("API nie zwróciło JSON. Sprawdź adres i proxy.");
  }

  return JSON.parse(text) as unknown;
}