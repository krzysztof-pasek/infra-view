import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import App from "../src/App";

vi.mock("../src/Charts", () => ({ Charts: () => null }));

const session = {
  id: 1,
  code: "TEST-1",
  firefighterName: "Jan Kowalski",
  description: null,
  location: null,
  startedAt: "2026-09-21T10:00:00Z",
  endedAt: null,
  status: "IN_PROGRESS",
};

const alarm = {
  id: 1,
  incidentId: 1,
  alarmType: "FALL",
  triggeredAt: session.startedAt,
  resolvedAt: null,
};

function mockApi({ failCreate = false } = {}) {
  let incidents = [session];
  let alarms = [alarm];

  const fetchMock = vi.fn(async (url: string, options: RequestInit = {}) => {
    const method = options.method ?? "GET";

    switch (`${method} ${url}`) {
      case "GET /api/incidents":
        return Response.json(incidents);
      case "GET /api/alarms":
        return Response.json(alarms);
      case "POST /api/incidents": {
        if (failCreate) return new Response(null, { status: 500 });
        const created = { ...JSON.parse(String(options.body)), id: 2 };
        incidents = [...incidents, created];
        return Response.json(created);
      }
      case "PUT /api/incidents/1/end": {
        const ended = { ...session, status: "RESOLVED", endedAt: session.startedAt };
        incidents = [ended];
        return Response.json(ended);
      }
      case "PUT /api/alarms/1/resolve": {
        alarms = [{ ...alarm, resolvedAt: session.startedAt }];
        return Response.json(alarms[0]);
      }
      default:
        return Response.json([]);
    }
  });

  vi.stubGlobal("fetch", fetchMock);
  vi.stubGlobal("matchMedia", () => ({ matches: false }));
  vi.spyOn(window, "confirm").mockReturnValue(true);
  return fetchMock;
}

function postCalls(fetchMock: ReturnType<typeof mockApi>) {
  return fetchMock.mock.calls.filter(([, options]) => options?.method === "POST");
}

async function fillNewSessionForm() {
  await screen.findByRole("button", { name: /Jan Kowalski/ });
  fireEvent.click(screen.getByText("Nowa sesja"));
  fireEvent.change(screen.getByLabelText("Kod sesji"), { target: { value: " TEST-2 " } });
  fireEvent.change(screen.getByLabelText("Imię i nazwisko"), { target: { value: " Anna Nowak " } });
}

it("tworzy sesję i czyści formularz", async () => {
  const fetchMock = mockApi();
  render(<App />);
  await fillNewSessionForm();

  fireEvent.click(screen.getByRole("button", { name: "Utwórz sesję" }));

  await screen.findByText("Sesja została utworzona.");
  expect(screen.getByRole("heading", { name: "Anna Nowak" })).toBeTruthy();
  expect(screen.getByLabelText<HTMLInputElement>("Kod sesji").value).toBe("");
  const body = JSON.parse(String(postCalls(fetchMock)[0][1]?.body));
  expect(body).toMatchObject({ code: "TEST-2", firefighterName: "Anna Nowak" });
});

it("po błędzie zapisu zostawia dane w formularzu", async () => {
  const fetchMock = mockApi({ failCreate: true });
  render(<App />);
  await fillNewSessionForm();

  fireEvent.click(screen.getByRole("button", { name: "Utwórz sesję" }));

  await screen.findByText(/HTTP 500/);
  expect(screen.getByLabelText<HTMLInputElement>("Kod sesji").value).toBe(" TEST-2 ");
  expect(postCalls(fetchMock)).toHaveLength(1);
});

it("nie kończy sesji, gdy operator anuluje", async () => {
  const fetchMock = mockApi();
  vi.mocked(window.confirm).mockReturnValue(false);
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));

  const urls = fetchMock.mock.calls.map(([url]) => url);
  expect(urls).not.toContain("/api/incidents/1/end");
});

it("kończy sesję po potwierdzeniu", async () => {
  mockApi();
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));

  await screen.findByText("Sesja została zakończona.");
  await waitFor(() => expect(screen.queryByRole("button", { name: /Jan Kowalski/ })).toBeNull());
});

it("rozwiązuje alarm", async () => {
  mockApi();
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Rozwiąż alarm" }));

  await screen.findByText("Alarm został rozwiązany.");
  expect(await screen.findByRole("heading", { name: "Aktywne alarmy (0)" })).toBeTruthy();
});