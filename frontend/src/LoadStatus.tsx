import { formatDate } from "./format";

type Props = {
  error: string | null;
  updatedAt: number | null;
  label: string;
};

export function LoadStatus({ error, updatedAt, label }: Props) {
  if (error) {
    const lastUpdate =
      updatedAt === null
        ? null
        : new Date(updatedAt).toISOString();

    return (
      <p className="error" role="alert">
        {label}: {error}. Ostatni poprawny odczyt API:{" "}
        {formatDate(lastUpdate)}
      </p>
    );
  }

  if (updatedAt === null) {
    return <p role="status">Ładowanie: {label}...</p>;
  }

  return null;
}