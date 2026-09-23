import { expect, it, vi } from "vitest";
import { request } from "../src/http";

function mockFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

it("zwraca odpowiedź JSON", async () => {
  mockFetch(Response.json({ id: 1 }));
  expect(await request("/incidents/1")).toEqual({ id: 1 });
});

it("zwraca null dla odpowiedzi 204", async () => {
  mockFetch(new Response(null, { status: 204 }));
  expect(await request("/incidents/1", { method: "DELETE" })).toBeNull();
});

it("zgłasza błąd HTTP i nie ponawia żądania", async () => {
  const fetchMock = mockFetch(new Response(null, { status: 500 }));

  await expect(request("/incidents", { method: "POST" })).rejects.toThrow("HTTP 500");
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

it("odrzuca HTML zamiast JSON", async () => {
  mockFetch(new Response("<html></html>", { headers: { "Content-Type": "text/html" } }));
  await expect(request("/incidents")).rejects.toThrow("API nie zwróciło JSON");
});