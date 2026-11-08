import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { App } from "../src/App";
import type { Incident, Telemetry } from "../src/types";
import { imagePath, jan, row } from "./fixtures";

vi.mock("../src/Charts", () => ({ Charts: ({ rows }: { rows: Telemetry[] }) => <p>Pomiary: {rows.length}</p> }));

const anna: Incident = {
  id: 3,
  code: "Anna Nowak",
  startedAt: "2026-09-20T10:00:00Z",
  endedAt: "2026-09-20T12:00:00Z",
  status: "RESOLVED",
};

type TestAlarm = {
  id: number;
  incidentId: number;
  alarmType: string;
  triggeredAt: string;
  resolvedAt: string | null;
};

function fall(id: number): TestAlarm {
  return { id, incidentId: 1, alarmType: "FALL", triggeredAt: jan.startedAt, resolvedAt: null };
}

const temperature = { metric: "TEMPERATURE", alarmType: "HIGH_TEMPERATURE", threshold: 80, resetMargin: 5, enabled: true };
const co2 = { metric: "CO2_PPM", alarmType: "HIGH_CO2", threshold: 5000, resetMargin: 500, enabled: false };
const tvoc = { metric: "TVOC_PPB", alarmType: "HIGH_TVOC", threshold: 2000, resetMargin: 200, enabled: true };

type Options = {
  failEnd?: boolean;
  serverDown?: boolean;
  alarmsDown?: boolean;
  alarms?: TestAlarm[];
  slowSave?: Promise<void>;
};

function requestName(url: string, init: RequestInit = {}) {
  let method = "GET";

  if (init.method !== undefined) {
    method = init.method;
  }

  return `${method} ${url}`;
}

function mockApi(options: Options = {}) {
  let incidents = [jan, anna];
  let alarms = [fall(1), fall(2), fall(3)];
  let readings = 0;

  if (options.alarms !== undefined) {
    alarms = options.alarms;
  }

  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    const request = requestName(url, init);

    if (options.serverDown) {
      throw new TypeError("Failed to fetch");
    }

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

    if (request.startsWith("GET /api/telemetry/incident/1")) {
      readings = readings + 1;
      return Response.json([row(0, readings)]);
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

    if (request === "PUT /api/incidents/1/code") {
      incidents = [{ ...jan, code: JSON.parse(String(init.body)).code }, anna];
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

    return Response.json([]);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function calls(fetchMock: ReturnType<typeof mockApi>, part: string) {
  const found = [];

  for (const call of fetchMock.mock.calls) {
    if (requestName(call[0], call[1]).includes(part)) {
      found.push(call);
    }
  }

  return found;
}

function body(fetchMock: ReturnType<typeof mockApi>, part: string) {
  const init = calls(fetchMock, part)[0][1];

  if (init === undefined) {
    return null;
  }

  return JSON.parse(String(init.body));
}

function findJan() {
  return screen.findByRole("button", { name: /^Jan Kowalski/ });
}

function optionsButton() {
  return screen.getByRole("button", { name: "Opcje sesji" });
}

async function openMenu() {
  fireEvent.click(await findJan());
  fireEvent.click(optionsButton());
  return screen.getByRole("menu");
}

async function chooseFromMenu(item: string) {
  fireEvent.click(within(await openMenu()).getByRole("menuitem", { name: item }));
}

function openDialog(button: string, title: string) {
  fireEvent.click(screen.getByRole("button", { name: button }));
  return screen.getByRole("dialog", { name: title });
}

function openThresholds() {
  return openDialog("Progi alarmów", "Progi alarmów");
}

function openArchive() {
  return openDialog("Archiwum", "Archiwum sesji");
}

async function firstLimit(dialog: HTMLElement) {
  const limits = await within(dialog).findAllByLabelText<HTMLInputElement>("Próg");
  return limits[0];
}

async function changeFirstLimit(dialog: HTMLElement) {
  fireEvent.change(await firstLimit(dialog), { target: { value: "60" } });
}

function noDialog() {
  return waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
}

function alarmList() {
  return screen.findByRole("region", { name: "Aktywne alarmy" });
}

beforeEach(() => {
  delete document.documentElement.dataset.theme;
  localStorage.clear();
});

it("grupuje powtórzone alarmy i rozwiązuje je jednym kliknięciem", async () => {
  const fetchMock = mockApi();
  render(<App />);
  const list = await alarmList();

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
  const list = await alarmList();
  await within(list).findByText("Upadek");
  await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());

  alarms.push({ ...fall(2), alarmType: "NO_MOTION" });
  const popup = await screen.findByRole("alert", {}, { timeout: 4000 });

  fireEvent.click(within(popup).getByRole("button", { name: "Pokaż" }));

  expect((await screen.findByRole("heading", { name: "Jan Kowalski" })).title).toBe("Jan Kowalski");
  await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
});

it("kończy sesję z menu dopiero po potwierdzeniu w oknie aplikacji", async () => {
  const fetchMock = mockApi();
  render(<App />);

  await chooseFromMenu("Zakończ sesję");
  const first = screen.getByRole("dialog", { name: "Zakończyć sesję?" });
  expect(first.querySelector(".modal-header button")).toBeNull();
  expect(first.lastElementChild).toBe(within(first).getByRole("button", { name: "Zamknij" }).parentElement);
  fireEvent.click(within(first).getByRole("button", { name: "Zamknij" }));
  await noDialog();
  expect(calls(fetchMock, "/end")).toHaveLength(0);

  fireEvent.click(optionsButton());
  fireEvent.click(screen.getByRole("menuitem", { name: "Zakończ sesję" }));
  const second = screen.getByRole("dialog", { name: "Zakończyć sesję?" });
  fireEvent.click(within(second).getByRole("button", { name: "Zakończ sesję" }));

  expect(await screen.findByText("Zakończona")).toBeTruthy();
  expect(screen.getByText("Pomiary: 1")).toBeTruthy();
  expect(calls(fetchMock, "/end")).toHaveLength(1);
});

it("Esc zamyka okno i oddaje fokus przyciskowi, który je otworzył", async () => {
  mockApi();
  render(<App />);
  await findJan();
  const button = screen.getByRole("button", { name: "Archiwum" });

  button.focus();
  fireEvent.click(button);
  const dialog = screen.getByRole("dialog", { name: "Archiwum sesji" });
  expect(dialog.getAttribute("aria-modal")).toBe("true");
  fireEvent.keyDown(dialog, { key: "Escape" });

  await noDialog();
  await waitFor(() => expect(document.activeElement).toBe(button));
});

it("otwarte okno wyłącza tło, a po zamknięciu je przywraca", () => {
  mockApi();
  render(<App />);
  const main = screen.getByRole("main");

  expect(main.closest("[inert]")).toBeNull();
  const dialog = openArchive();
  expect(main.closest("[inert]")).not.toBeNull();
  expect(dialog.closest("[inert]")).toBeNull();

  fireEvent.keyDown(dialog, { key: "Escape" });
  expect(main.closest("[inert]")).toBeNull();
});

it("pauza zatrzymuje obraz na klatce, a wznowienie cofa go o czas pauzy", async () => {
  mockApi();
  render(<App />);
  fireEvent.click(await findJan());
  expect(screen.getByText("Brak obrazu z kamery")).toBeTruthy();
  await waitFor(() => expect(imagePath()).toBe("/api/incidents/1/live?connection=1"), { timeout: 3000 });

  vi.useFakeTimers({ toFake: ["Date"] });
  fireEvent.click(screen.getByRole("button", { name: "Wstrzymaj" }));
  expect(imagePath()).toContain("/api/incidents/1/frame?at=");
  vi.setSystemTime(Date.now() + 12000);
  fireEvent.click(screen.getByRole("button", { name: "Wznów" }));

  await waitFor(() => expect(imagePath()).toBe("/api/incidents/1/replay?delaySeconds=12&connection=1"));
});

it("błąd operacji zostaje na ekranie do zamknięcia", async () => {
  mockApi({ failEnd: true });
  render(<App />);

  await chooseFromMenu("Zakończ sesję");
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Zakończ sesję" }));
  const message = await screen.findByText("Serwer odrzucił żądanie (błąd 500)");

  fireEvent.click(within(screen.getByRole("alert")).getByRole("button", { name: "Zamknij" }));

  await waitFor(() => expect(message.isConnected).toBe(false));
});

it("archiwum filtruje sesje, odtwarza nagranie zakończonej od początku i pobiera do niego dłuższą historię pomiarów", async () => {
  const fetchMock = mockApi();
  vi.useFakeTimers({ toFake: ["Date"] });
  render(<App />);
  await findJan();
  await waitFor(() => expect(screen.queryByRole("button", { name: /^Anna Nowak/ })).toBeNull());

  const archive = openArchive();
  fireEvent.change(within(archive).getByLabelText("Szukaj"), { target: { value: "anna" } });

  expect(within(archive).queryByText("Jan Kowalski")).toBeNull();
  fireEvent.click(within(archive).getByRole("button", { name: "Otwórz" }));

  expect(await screen.findByRole("heading", { name: "Anna Nowak" })).toBeTruthy();
  expect(imagePath()).toBe("/api/incidents/3/recording?from=2026-09-20T10:00:00.000Z");
  await waitFor(() => expect(screen.queryByRole("button", { name: "Na żywo" })).toBeNull());
  expect(screen.getByRole("button", { name: "Wstrzymaj" })).toBeTruthy();
  expect(screen.getByText("0:00")).toBeTruthy();
  expect(calls(fetchMock, "GET /api/telemetry/incident/3?limit=3600")).toHaveLength(1);
});

it("jednym przyciskiem zapisuje tylko zmienione progi alarmów", async () => {
  const fetchMock = mockApi();
  render(<App />);

  const dialog = openThresholds();
  await changeFirstLimit(dialog);
  fireEvent.click(within(dialog).getAllByLabelText("Włączony")[1]);

  expect(within(dialog).getAllByRole("button", { name: "Zapisz" })).toHaveLength(1);
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));

  await waitFor(() => expect(calls(fetchMock, "PUT /api/alarm-thresholds/CO2_PPM")).toHaveLength(1));
  expect(body(fetchMock, "PUT /api/alarm-thresholds/TEMPERATURE")).toEqual({ threshold: 60, resetMargin: 5, enabled: true });
  expect(body(fetchMock, "PUT /api/alarm-thresholds/CO2_PPM")).toEqual({ threshold: 5000, resetMargin: 500, enabled: true });
  expect(calls(fetchMock, "PUT /api/alarm-thresholds/TVOC_PPB")).toHaveLength(0);
  await noDialog();
});

it("pyta przed porzuceniem niezapisanych progów", async () => {
  mockApi();
  render(<App />);

  const dialog = openThresholds();
  const limit = await firstLimit(dialog);
  fireEvent.keyDown(dialog, { key: "Escape" });
  await noDialog();

  const again = openThresholds();
  await changeFirstLimit(again);
  fireEvent.keyDown(again, { key: "Escape" });
  const question = screen.getByRole("dialog", { name: "Porzucić zmiany?" });
  fireEvent.keyDown(question, { key: "Escape" });

  await waitFor(() => expect(screen.queryByRole("dialog", { name: "Porzucić zmiany?" })).toBeNull());
  expect((await firstLimit(again)).value).toBe("60");
  fireEvent.click(within(again).getByRole("button", { name: "Zamknij" }));
  fireEvent.click(within(screen.getByRole("dialog", { name: "Porzucić zmiany?" })).getByRole("button", { name: "Porzuć" }));

  await noDialog();
  await waitFor(() => expect(limit.isConnected).toBe(false));
});

it("zmienia nazwę sesji z menu", async () => {
  const fetchMock = mockApi();
  render(<App />);

  await chooseFromMenu("Zmień nazwę");
  const dialog = screen.getByRole("dialog", { name: "Zmień nazwę sesji" });
  const input = within(dialog).getByLabelText("Nazwa sesji");
  expect(document.activeElement).toBe(input);

  fireEvent.change(input, { target: { value: "   " } });
  expect(within(dialog).getByRole("button", { name: "Zapisz" })).toHaveProperty("disabled", true);
  fireEvent.change(input, { target: { value: " Zastęp 2 " } });
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));

  await noDialog();
  expect(body(fetchMock, "PUT /api/incidents/1/code")).toEqual({ code: "Zastęp 2" });
  expect(await screen.findByRole("heading", { name: "Zastęp 2" })).toBeTruthy();
});

it("menu sesji zamyka się kliknięciem obok i klawiszem Esc", async () => {
  mockApi();
  render(<App />);

  await openMenu();
  fireEvent.click(document.body);
  await waitFor(() => expect(screen.queryByRole("menu")).toBeNull());

  fireEvent.click(optionsButton());
  expect(screen.getByRole("menu")).toBeTruthy();
  fireEvent.keyDown(document, { key: "Escape" });
  await waitFor(() => expect(screen.queryByRole("menu")).toBeNull());
  expect(document.activeElement).toBe(optionsButton());

  fireEvent.click(optionsButton());
  fireEvent.click(screen.getByRole("menuitem", { name: "Zamknij" }));
  await waitFor(() => expect(screen.queryByRole("heading", { name: "Jan Kowalski" })).toBeNull());
});

it("menu sesji obsługuje się klawiaturą", async () => {
  mockApi();
  render(<App />);
  const menu = await openMenu();
  const items = within(menu).getAllByRole("menuitem");

  expect(document.activeElement).toBe(items[0]);
  expect(fireEvent.keyDown(document, { key: "ArrowDown" })).toBe(false);
  expect(document.activeElement).toBe(items[1]);
  fireEvent.keyDown(document, { key: "ArrowUp" });
  fireEvent.keyDown(document, { key: "ArrowUp" });
  expect(document.activeElement).toBe(items[2]);

  fireEvent.keyDown(document, { key: "Tab" });
  expect(document.activeElement).toBe(optionsButton());
  await waitFor(() => expect(screen.queryByRole("menu")).toBeNull());
});

it("gdy serwer nie odpowiada, nie twierdzi że alarmów nie ma", async () => {
  mockApi({ alarmsDown: true });
  render(<App />);
  const list = await alarmList();

  expect(await within(list).findByText("Brak połączenia z serwerem")).toBeTruthy();
  expect(within(list).queryByText("Brak aktywnych alarmów")).toBeNull();
});

it("gdy serwer nie odpowiada, nie udaje że lista sesji się ładuje", async () => {
  mockApi({ serverDown: true });
  render(<App />);

  await waitFor(() => expect(screen.getAllByText("Brak połączenia z serwerem")).toHaveLength(2));
  expect(screen.queryByText("Ładowanie…")).toBeNull();
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

  const dialog = openThresholds();
  const overlay = screen.getByRole("dialog", { name: "Progi alarmów" }).closest(".overlay");
  fireEvent.mouseDown(await firstLimit(dialog));

  if (overlay !== null) {
    fireEvent.mouseUp(overlay);
    fireEvent.click(overlay);
    expect(screen.getByRole("dialog", { name: "Progi alarmów" })).toBeTruthy();
    fireEvent.mouseDown(overlay);
  }

  await noDialog();
});

it("spóźniony zapis progów nie zamyka innego okna", async () => {
  let finish = () => {};
  const slowSave = new Promise<void>(resolve => {
    finish = resolve;
  });
  const fetchMock = mockApi({ slowSave });
  render(<App />);

  const dialog = openThresholds();
  await changeFirstLimit(dialog);
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
  const list = await alarmList();
  await within(list).findByText("Upadek");

  const popups = await screen.findAllByRole("alert");
  expect(popups).toHaveLength(1);
  expect(within(popups[0]).getByText("SOS")).toBeTruthy();
});

it("zmiana nazwy dotyczy sesji, z której otwarto okno, nawet po przejściu do alarmu", async () => {
  const alarms = [fall(1)];
  const fetchMock = mockApi({ alarms });
  render(<App />);
  await screen.findByText("Upadek");

  await chooseFromMenu("Zmień nazwę");
  alarms.push({ ...fall(2), incidentId: 3, alarmType: "SOS" });
  fireEvent.click(within(await screen.findByRole("alert", {}, { timeout: 4000 })).getByRole("button", { name: "Pokaż" }));
  expect(await screen.findByRole("heading", { name: "Anna Nowak" })).toBeTruthy();

  const dialog = screen.getByRole("dialog", { name: "Zmień nazwę sesji" });
  fireEvent.change(within(dialog).getByLabelText("Nazwa sesji"), { target: { value: "Alfa" } });
  fireEvent.click(within(dialog).getByRole("button", { name: "Zapisz" }));

  await waitFor(() => expect(calls(fetchMock, "PUT /api/incidents/1/code")).toHaveLength(1));
  expect(calls(fetchMock, "PUT /api/incidents/3/code")).toHaveLength(0);
});

it("Esc zamyka okno także wtedy, gdy fokus wypadł poza okno", async () => {
  mockApi();
  render(<App />);
  await findJan();

  fireEvent.click(screen.getByRole("button", { name: "Archiwum" }));
  within(screen.getByRole("dialog")).getByRole("button", { name: "Zamknij" }).blur();
  expect(document.activeElement).toBe(document.body);
  fireEvent.keyDown(document.body, { key: "Escape" });

  await noDialog();
});

it("okno otwarte zaraz po zamknięciu startuje od nowa", async () => {
  mockApi();
  render(<App />);

  const dialog = openThresholds();
  await changeFirstLimit(dialog);
  fireEvent.keyDown(dialog, { key: "Escape" });
  fireEvent.click(await screen.findByRole("button", { name: "Porzuć" }));
  fireEvent.click(screen.getByRole("button", { name: "Progi alarmów" }));

  await waitFor(() => expect(screen.getAllByRole("dialog")).toHaveLength(1));
  const again = screen.getByRole("dialog", { name: "Progi alarmów" });
  expect((await firstLimit(again)).value).toBe("80");
  expect(screen.queryByRole("dialog", { name: "Porzucić zmiany?" })).toBeNull();
});
