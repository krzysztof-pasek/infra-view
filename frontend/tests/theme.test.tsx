import { act, renderHook } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { useTheme } from "../src/useTheme";

it("zmienia motyw, nawet gdy localStorage jest zablokowany", () => {
  const blocked = () => {
    throw new DOMException("Storage blocked", "SecurityError");
  };
  vi.spyOn(Storage.prototype, "getItem").mockImplementation(blocked);
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(blocked);
  vi.stubGlobal("matchMedia", () => ({ matches: true }));

  const { result } = renderHook(() => useTheme());
  expect(result.current.theme).toBe("dark");

  act(() => result.current.toggleTheme());
  expect(document.documentElement.dataset.theme).toBe("light");
});