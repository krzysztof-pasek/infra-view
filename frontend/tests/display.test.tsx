import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { Metrics } from "../src/Metrics";
import { RecordingList } from "../src/RecordingList";

it("zakończona sesja bez pomiarów pokazuje brak pomiarów", () => {
  render(<Metrics latest={undefined} now={0} ended />);

  expect(screen.getByText("Brak pomiarów")).toBeTruthy();
  expect(screen.queryByText("Dane historyczne")).toBeNull();
});

it("zakończona sesja z pomiarem pokazuje dane historyczne", () => {
  const measurement = {
    id: 1,
    incidentId: 1,
    recordedAt: "2026-09-21T10:00:00Z",
    temperature: 0,
    gasPpm: null,
    co2Ppm: null,
    motionState: null,
  };
  render(<Metrics latest={measurement} now={0} ended />);

  expect(screen.getByText("Dane historyczne")).toBeTruthy();
  expect(screen.getByText("0")).toBeTruthy();
});

it("nie dopisuje jednostek, gdy brakuje danych nagrania", () => {
  const recording = {
    id: 1,
    incidentId: 1,
    startedAt: "2026-09-21T10:00:00Z",
    endedAt: null,
    filePath: null,
    durationSec: null,
    fileSizeBytes: null,
  };
  render(<RecordingList recordings={[recording]} />);

  expect(screen.queryByText("Brak danych s")).toBeNull();
  expect(screen.queryByText("Brak danych MiB")).toBeNull();
});