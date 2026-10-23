const TIMEOUT = 5000;

export async function api<T>(path: string, method = "GET", body?: unknown): Promise<T> {
  const options: RequestInit = { method, signal: AbortSignal.timeout(TIMEOUT) };

  if (body !== undefined) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }

  let response: Response;

  try {
    response = await fetch("/api" + path, options);
  } catch {
    throw new Error("Brak połączenia z serwerem");
  }

  if (!response.ok) {
    throw new Error(`Serwer odrzucił żądanie (błąd ${response.status})`);
  }

  return response.json();
}
