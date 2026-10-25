import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import App from "../src/App";

vi.mock("../src/Charts", () => ({ Charts: () => null }));

const jan = {
  id: 1,
  code: "HELMET-1",
  firefighterName: "Jan Kowalski",
  startedAt: "2026-09-21T10:00:00Z",
  endedAt: null,
  status: "IN_PROGRESS",
};

const anna = {
  id: 3,
  code: "HELMET-3",
  firefighterName: "Anna Nowak",
  startedAt: "2026-09-20T10:00:00Z",
  endedAt: "2026-09-20T12:00:00Z",
  status: "RESOLVED",
};

function fall(id: number) {
  return { id, incidentId: 1, alarmType: "FALL", triggeredAt: jan.startedAt, resolvedAt: null as string | null };
}

const temperature = { metric: "TEMPERATURE", alarmType: "HIGH_TEMPERATURE", threshold: 80, resetMargin: 5, enabled: true };

type Options = {
  failEnd?: boolean;
  alarmsDown?: boolean;
  alarms?: ReturnType<typeof fall>[];
};

function mockApi(options: Options = {}) {
  let incidents = [jan, anna];
  let alarms = [fall(1), fall(2), fall(3)];

  if (options.alarms !== undefined) {
    alarms = options.alarms;
  }

  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    let method = "GET";

    if (init.method !== undefined) {
      method = init.method;
    }

    const request = method + " " + url;

    if (request === "GET /api/incidents") {
      return Response.json(incidents);
    }

    if (request === "GET /api/alarms") {
      if (options.alarmsDown) {
        throw new TypeError("Failed to fetch");
      }

      return Response.json(alarms);
    }

    if (request === "GET /api/alarm-thresholds") {
      return Response.json([temperature]);
    }

    if (request === "PUT /api/incidents/1/end") {
      if (options.failEnd) {
        return new Response(null, { status: 500 });
      }

      incidents = [{ ...jan, status: "RESOLVED", endedAt: jan.startedAt }, anna];
      return Response.json(incidents[0]);
    }

    if (request.startsWith("PUT /api/alarms/")) {
      for (const alarm of alarms) {
        if (url === `/api/alarms/${alarm.id}/resolve`) {
          alarm.resolvedAt = jan.startedAt;
        }
      }

      return Response.json({});
    }

    if (request === "PUT /api/alarm-thresholds/TEMPERATURE") {
      return Response.json({ ...temperature, ...JSON.parse(String(init.body)) });
    }

    if (url.includes("/live")) {
      return new Response(null, { status: 404 });
    }

    return Response.json([]);
  });

  vi.stubGlobal("fetch", fetchMock);
  vi.spyOn(window, "confirm").mockReturnValue(true);
  return fetchMock;
}

function calls(fetchMock: ReturnType<typeof mockApi>, method: string, part: string) {
  const found = [];

  for (const call of fetchMock.mock.calls) {
    const init = call[1];

    if (init !== undefined && init.method === method && call[0].includes(part)) {
      found.push(call);
    }
  }

  return found;
}

beforeEach(() => {
  delete document.documentElement.dataset.theme;
  localStorage.clear();
});

it("grupuje powtórzone alarmy i rozwiązuje je jednym kliknięciem", async () => {
  const fetchMock = mockApi();
  render(<App />);
  const list = await screen.findByRole("region", { name: "Aktywne alarmy" });

  expect(await within(list).findByText("Upadek ×3")).toBeTruthy();
  fireEvent.click(within(list).getByRole("button", { name: "Rozwiąż" }));

  await waitFor(() => expect(calls(fetchMock, "PUT", "/resolve")).toHaveLength(3));
  expect(await within(list).findByText("Brak aktywnych alarmów")).toBeTruthy();
});

it("kończy sesję dopiero po potwierdzeniu", async () => {
  const fetchMock = mockApi();
  vi.mocked(window.confirm).mockReturnValue(false);
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));
  expect(calls(fetchMock, "PUT", "/end")).toHaveLength(0);

  vi.mocked(window.confirm).mockReturnValue(true);
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));

  expect(await screen.findByText("Zakończona")).toBeTruthy();
  expect(calls(fetchMock, "PUT", "/end")).toHaveLength(1);
});

it("błąd operacji zostaje na ekranie do zamknięcia", async () => {
  mockApi({ failEnd: true });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));
  const message = await screen.findByText("Serwer odrzucił żądanie (błąd 500)");

  fireEvent.click(within(message.parentElement as HTMLElement).getByRole("button", { name: "Zamknij" }));

  expect(screen.queryByText("Serwer odrzucił żądanie (błąd 500)")).toBeNull();
});

it("zapisuje nowy próg alarmu", async () => {
  const fetchMock = mockApi();
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const dialog = screen.getByRole("dialog", { name: "Progi alarmów" });
  fireEvent.change(await within(dialog).findByLabelText("Próg"), { target: { value: "60" } });
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));

  await waitFor(() => expect(calls(fetchMock, "PUT", "/alarm-thresholds/TEMPERATURE")).toHaveLength(1));
  const body = JSON.parse(String(calls(fetchMock, "PUT", "/alarm-thresholds/TEMPERATURE")[0][1]?.body));
  expect(body).toEqual({ threshold: 60, resetMargin: 5, enabled: true });
});

it("gdy serwer nie odpowiada, nie twierdzi że alarmów nie ma", async () => {
  mockApi({ alarmsDown: true });
  render(<App />);
  const list = await screen.findByRole("region", { name: "Aktywne alarmy" });

  expect(await within(list).findByText("Brak połączenia z serwerem")).toBeTruthy();
  expect(within(list).queryByText("Brak aktywnych alarmów")).toBeNull();
});

it("przełącza motyw i zapamiętuje wybór", () => {
  mockApi();
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Ciemny motyw" }));

  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(localStorage.getItem("theme")).toBe("dark");
  expect(screen.getByRole("button", { name: "Jasny motyw" })).toBeTruthy();
});
