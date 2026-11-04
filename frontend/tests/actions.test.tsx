import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import App from "../src/App";

vi.mock("../src/Charts", () => ({ Charts: () => null }));

const jan = {
  id: 1,
  code: "Jan Kowalski",
  startedAt: "2026-09-21T10:00:00Z",
  endedAt: null,
  status: "IN_PROGRESS",
};

const anna = {
  id: 3,
  code: "Anna Nowak",
  startedAt: "2026-09-20T10:00:00Z",
  endedAt: "2026-09-20T12:00:00Z",
  status: "RESOLVED",
};

function fall(id: number) {
  return { id, incidentId: 1, alarmType: "FALL", triggeredAt: jan.startedAt, resolvedAt: null as string | null };
}

const temperature = { metric: "TEMPERATURE", alarmType: "HIGH_TEMPERATURE", threshold: 80, resetMargin: 5, enabled: true };
const co2 = { metric: "CO2_PPM", alarmType: "HIGH_CO2", threshold: 5000, resetMargin: 500, enabled: false };
const tvoc = { metric: "TVOC_PPB", alarmType: "HIGH_TVOC", threshold: 2000, resetMargin: 200, enabled: true };

type Options = {
  failEnd?: boolean;
  alarmsDown?: boolean;
  alarms?: ReturnType<typeof fall>[];
  slowSave?: Promise<void>;
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

    if (request === "GET /api/alarms/active") {
      if (options.alarmsDown) {
        throw new TypeError("Failed to fetch");
      }

      const unresolved = [];

      for (const alarm of alarms) {
        if (alarm.resolvedAt === null) {
          unresolved.push(alarm);
        }
      }

      return Response.json(unresolved);
    }

    if (request === "GET /api/alarm-thresholds") {
      return Response.json([temperature, co2, tvoc]);
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

    if (request.startsWith("PUT /api/alarm-thresholds/")) {
      if (options.slowSave !== undefined) {
        await options.slowSave;
      }

      return Response.json(JSON.parse(String(init.body)));
    }

    if (url.includes("/live")) {
      return new Response(null, { status: 404 });
    }

    return Response.json([]);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function calls(fetchMock: ReturnType<typeof mockApi>, part: string) {
  const found = [];

  for (const call of fetchMock.mock.calls) {
    let method = "GET";

    if (call[1] !== undefined && call[1].method !== undefined) {
      method = call[1].method;
    }

    if ((method + " " + call[0]).includes(part)) {
      found.push(call);
    }
  }

  return found;
}

function body(fetchMock: ReturnType<typeof mockApi>, part: string) {
  return JSON.parse(String(calls(fetchMock, part)[0][1]?.body));
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
  const dialog = screen.getByRole("dialog", { name: "Rozwiązać alarm?" });
  expect(calls(fetchMock, "/resolve")).toHaveLength(0);
  fireEvent.click(within(dialog).getByRole("button", { name: "Rozwiąż" }));

  await waitFor(() => expect(calls(fetchMock, "/resolve")).toHaveLength(3));
  expect(await within(list).findByText("Brak aktywnych alarmów")).toBeTruthy();
});

it("nowy alarm pojawia się jako popup, który otwiera sesję strażaka", async () => {
  const alarms = [fall(1)];
  mockApi({ alarms });
  render(<App />);
  const list = await screen.findByRole("region", { name: "Aktywne alarmy" });
  await within(list).findByText("Upadek");
  expect(screen.queryByRole("alert")).toBeNull();

  alarms.push({ ...fall(2), alarmType: "NO_MOTION" });
  const popup = await screen.findByRole("alert", {}, { timeout: 4000 });

  fireEvent.click(within(popup).getByRole("button", { name: "Pokaż" }));

  expect((await screen.findByRole("heading", { name: "Jan Kowalski" })).title).toBe("Jan Kowalski");
  expect(screen.queryByRole("alert")).toBeNull();
});

it("kończy sesję dopiero po potwierdzeniu w oknie aplikacji", async () => {
  const fetchMock = mockApi();
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));
  const first = screen.getByRole("dialog", { name: "Zakończyć sesję?" });
  expect(first.querySelector(".modal-header button")).toBeNull();
  expect(first.lastElementChild).toBe(within(first).getByRole("button", { name: "Zamknij" }).parentElement);
  fireEvent.click(within(first).getByRole("button", { name: "Zamknij" }));
  expect(screen.queryByRole("dialog")).toBeNull();
  expect(calls(fetchMock, "/end")).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));
  const second = screen.getByRole("dialog", { name: "Zakończyć sesję?" });
  fireEvent.click(within(second).getByRole("button", { name: "Zakończ sesję" }));

  expect(await screen.findByText("Zakończona")).toBeTruthy();
  expect(calls(fetchMock, "/end")).toHaveLength(1);
});

it("Esc zamyka okno i oddaje fokus przyciskowi, który je otworzył", async () => {
  mockApi();
  render(<App />);
  await screen.findByRole("button", { name: /^Jan Kowalski/ });
  const button = screen.getByRole("button", { name: "Archiwum" });

  button.focus();
  fireEvent.click(button);
  const dialog = screen.getByRole("dialog", { name: "Archiwum sesji" });
  expect(dialog.getAttribute("aria-modal")).toBe("true");
  fireEvent.keyDown(dialog, { key: "Escape" });

  expect(screen.queryByRole("dialog")).toBeNull();
  await waitFor(() => expect(document.activeElement).toBe(button));
});

it("pauza zatrzymuje obraz, a wznowienie cofa go o czas pauzy", async () => {
  const fetchMock = mockApi();
  render(<App />);
  fireEvent.click(await screen.findByRole("button", { name: /^Jan Kowalski/ }));
  await waitFor(() => expect(calls(fetchMock, "GET /api/incidents/1/").length).toBeGreaterThan(0));

  vi.useFakeTimers({ toFake: ["Date"] });
  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));
  const paused = calls(fetchMock, "GET /api/incidents/1/").length;
  vi.setSystemTime(Date.now() + 12000);
  await new Promise(resolve => setTimeout(resolve, 2500));
  expect(calls(fetchMock, "GET /api/incidents/1/")).toHaveLength(paused);

  fireEvent.click(screen.getByRole("button", { name: "Wznów" }));
  await waitFor(() => expect(calls(fetchMock, "GET /api/incidents/1/").at(-1)?.[0]).toContain("/replay?delaySeconds=12"));
  vi.useRealTimers();
});

it("błąd operacji zostaje na ekranie do zamknięcia", async () => {
  mockApi({ failEnd: true });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: /^Jan Kowalski/ }));
  fireEvent.click(screen.getByRole("button", { name: "Zakończ sesję" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Zakończ sesję" }));
  const message = await screen.findByText("Serwer odrzucił żądanie (błąd 500)");

  fireEvent.click(within(message.parentElement as HTMLElement).getByRole("button", { name: "Zamknij" }));

  expect(screen.queryByText("Serwer odrzucił żądanie (błąd 500)")).toBeNull();
});

it("archiwum filtruje sesje i otwiera zakończoną", async () => {
  mockApi();
  render(<App />);
  await screen.findByRole("button", { name: /^Jan Kowalski/ });
  expect(screen.queryByRole("button", { name: /^Anna Nowak/ })).toBeNull();

  fireEvent.click(screen.getByRole("button", { name: "Archiwum" }));
  const archive = screen.getByRole("dialog", { name: "Archiwum sesji" });
  fireEvent.change(within(archive).getByLabelText("Szukaj"), { target: { value: "anna" } });

  expect(within(archive).queryByText("Jan Kowalski")).toBeNull();
  fireEvent.click(within(archive).getByRole("button", { name: "Otwórz" }));

  expect(await screen.findByRole("heading", { name: "Anna Nowak" })).toBeTruthy();
  expect(screen.getByText("Sesja zakończona, obraz na żywo jest niedostępny")).toBeTruthy();
});

it("jednym przyciskiem zapisuje tylko zmienione progi alarmów", async () => {
  const fetchMock = mockApi();
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const dialog = screen.getByRole("dialog", { name: "Progi alarmów" });
  const limits = await within(dialog).findAllByLabelText("Próg");
  fireEvent.change(limits[0], { target: { value: "60" } });
  fireEvent.click(within(dialog).getAllByLabelText("Włączony")[1]);

  expect(within(dialog).getAllByRole("button", { name: "Zapisz" })).toHaveLength(1);
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));

  await waitFor(() => expect(calls(fetchMock, "PUT /api/alarm-thresholds/CO2_PPM")).toHaveLength(1));
  expect(body(fetchMock, "PUT /api/alarm-thresholds/TEMPERATURE")).toEqual({ threshold: 60, resetMargin: 5, enabled: true });
  expect(body(fetchMock, "PUT /api/alarm-thresholds/CO2_PPM")).toEqual({ threshold: 5000, resetMargin: 500, enabled: true });
  expect(calls(fetchMock, "PUT /api/alarm-thresholds/TVOC_PPB")).toHaveLength(0);
  await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
});

it("pyta przed porzuceniem niezapisanych progów", async () => {
  mockApi();
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const dialog = screen.getByRole("dialog", { name: "Progi alarmów" });
  const limits = await within(dialog).findAllByLabelText("Próg");
  fireEvent.keyDown(dialog, { key: "Escape" });
  expect(screen.queryByRole("dialog")).toBeNull();

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const again = screen.getByRole("dialog", { name: "Progi alarmów" });
  fireEvent.change((await within(again).findAllByLabelText("Próg"))[0], { target: { value: "60" } });
  fireEvent.keyDown(again, { key: "Escape" });
  const question = screen.getByRole("dialog", { name: "Porzucić zmiany?" });
  fireEvent.keyDown(question, { key: "Escape" });

  expect(screen.queryByRole("dialog", { name: "Porzucić zmiany?" })).toBeNull();
  expect((within(again).getAllByLabelText("Próg")[0] as HTMLInputElement).value).toBe("60");
  fireEvent.click(within(again).getByRole("button", { name: "Zamknij" }));
  fireEvent.click(within(screen.getByRole("dialog", { name: "Porzucić zmiany?" })).getByRole("button", { name: "Porzuć" }));

  expect(screen.queryByRole("dialog")).toBeNull();
  expect(limits[0].isConnected).toBe(false);
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

  fireEvent.click(screen.getByRole("button", { name: "Włącz ciemny motyw" }));

  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(localStorage.getItem("theme")).toBe("dark");
  expect(screen.getByRole("button", { name: "Włącz jasny motyw" })).toBeTruthy();
});

it("przeciągnięcie myszy z pola na tło nie zamyka okna", async () => {
  mockApi();
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const dialog = screen.getByRole("dialog", { name: "Progi alarmów" });
  const limits = await within(dialog).findAllByLabelText("Próg");
  const overlay = dialog.parentElement as HTMLElement;
  fireEvent.mouseDown(limits[0]);
  fireEvent.mouseUp(overlay);
  fireEvent.click(overlay);

  expect(screen.getByRole("dialog", { name: "Progi alarmów" })).toBeTruthy();
  fireEvent.mouseDown(overlay);
  expect(screen.queryByRole("dialog")).toBeNull();
});

it("spóźniony zapis progów nie zamyka innego okna", async () => {
  let finish = () => {};
  const slowSave = new Promise<void>(resolve => {
    finish = resolve;
  });
  const fetchMock = mockApi({ slowSave });
  render(<App />);

  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));
  const dialog = screen.getByRole("dialog", { name: "Progi alarmów" });
  fireEvent.change((await within(dialog).findAllByLabelText("Próg"))[0], { target: { value: "60" } });
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));
  await waitFor(() => expect(calls(fetchMock, "PUT /api/alarm-thresholds/")).toHaveLength(1));
  fireEvent.keyDown(dialog, { key: "Escape" });
  fireEvent.click(screen.getByRole("button", { name: "Porzuć" }));
  fireEvent.click(screen.getByRole("button", { name: "Archiwum" }));
  finish();

  await waitFor(() => expect(screen.getByRole("button", { name: "Archiwum" })).toHaveProperty("disabled", false));
  await new Promise(resolve => setTimeout(resolve, 50));
  expect(screen.getByRole("dialog", { name: "Archiwum sesji" })).toBeTruthy();
});

it("SOS trwający przy otwarciu strony pokazuje okienko", async () => {
  mockApi({ alarms: [fall(1), { ...fall(2), alarmType: "SOS" }] });
  render(<App />);
  const list = await screen.findByRole("region", { name: "Aktywne alarmy" });
  await within(list).findByText("Upadek");

  const popups = await screen.findAllByRole("alert");
  expect(popups).toHaveLength(1);
  expect(within(popups[0]).getByText("SOS")).toBeTruthy();
});

it("Esc zamyka okno także wtedy, gdy fokus wypadł poza okno", async () => {
  mockApi();
  render(<App />);
  await screen.findByRole("button", { name: /^Jan Kowalski/ });

  fireEvent.click(screen.getByRole("button", { name: "Archiwum" }));
  (document.activeElement as HTMLElement).blur();
  fireEvent.keyDown(document.body, { key: "Escape" });

  expect(screen.queryByRole("dialog")).toBeNull();
});
