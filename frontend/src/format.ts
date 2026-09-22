export function formatNumber(
  value: number | null | undefined,
  digits = 1
) {
  if (value == null || !Number.isFinite(value)) {
    return "Brak danych";
  }

  return value.toLocaleString("pl-PL", {
    maximumFractionDigits: digits,
  });
}

export function formatDate(value: string | null | undefined) {
  if (!value || !Number.isFinite(Date.parse(value))) {
    return "Brak danych";
  }

  return new Date(value).toLocaleString("pl-PL");
}

export function freshness(recordedAt: string | undefined, now: number) {
  if (!recordedAt) {
    return "Brak pomiarów";
  }

  const age = now - Date.parse(recordedAt);

  if (!Number.isFinite(age)) {
    return "Nieprawidłowy czas";
  }

  if (age < -5000) {
    return "Sprawdź zegar urządzenia";
  }

  if (age > 5000) {
    return "Brak nowych pomiarów";
  }

  return "Świeży pomiar";
}