type Props = {
  message: string;
  onClose: () => void;
};

export function Toast({ message, onClose }: Props) {
  if (message === "") {
    return null;
  }

  return (
    <div className="toast glass" role="alert">
      <span className="orange">{message}</span>
      <button onClick={onClose}>Zamknij</button>
    </div>
  );
}
