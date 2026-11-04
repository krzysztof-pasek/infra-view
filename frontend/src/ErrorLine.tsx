import { errorText } from "./format";

type Props = {
  error: string;
  updatedAt?: number;
};

export function ErrorLine({ error, updatedAt = 0 }: Props) {
  if (error === "") {
    return null;
  }

  return <p className="orange">{errorText(error, updatedAt)}</p>;
}
