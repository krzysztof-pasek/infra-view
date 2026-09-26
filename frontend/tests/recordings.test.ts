import { expect, it, vi } from "vitest";
import { getRecordings } from "../src/api";
import { parseRecording } from "../src/parsers";

const recording = {
  id: 1,
  incidentId: 1,
  startedAt: "2026-09-21T10:00:00Z",
  endedAt: null,
  filePath: "/recordings/1.mjpeg",
  fileSizeBytes: null,
  durationSec: null,
};

it("przyjmuje nagranie z polem filePath", () => {
  expect(parseRecording(recording)).toEqual(recording);
});

it("odrzuca stare pole storageKey", () => {
  const old = { ...recording, filePath: undefined, storageKey: "old-key" };
  expect(() => parseRecording(old)).toThrow();
});

it("pobiera nagrania sesji i pomija nagrania innej sesji", async () => {
  const other = { ...recording, id: 2, incidentId: 2 };
  const fetchMock = vi.fn().mockResolvedValue(Response.json([recording, other]));
  vi.stubGlobal("fetch", fetchMock);

  const result = await getRecordings(1, new AbortController().signal);

  expect(result).toEqual([recording]);
  expect(fetchMock.mock.calls[0][0]).toBe("/api/videos/incident/1");
});