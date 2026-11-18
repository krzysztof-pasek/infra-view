const TIMEOUT = 5000;

export async function api<T>(path: string, method = "GET", body: unknown = null): Promise<T | null> {
  const options: RequestInit = { method, signal: AbortSignal.timeout(TIMEOUT) };

  if (body !== null) {
    options.headers = { "Content-Type": "application/json" };
    options.body = JSON.stringify(body);
  }

  let response: Response;

  try {
    response = await fetch(`/api${path}`, options);
  } catch {
    throw new Error("Brak połączenia z serwerem");
  }

  if (!response.ok) {
    throw new Error(`Serwer odrzucił żądanie (błąd ${response.status})`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export function errorMessage(error: unknown, fallback: string) {
  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}
