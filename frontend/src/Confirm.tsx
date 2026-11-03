import { Modal } from "./Modal";

type Props = {
  title: string;
  text: string;
  button: string;
  onYes: () => void;
  onNo: () => void;
};

export function Confirm({ title, text, button, onYes, onNo }: Props) {
  const yes = (
    <button className="primary" onClick={onYes}>
      {button}
    </button>
  );

  return (
    <Modal title={title} action={yes} alert onClose={onNo}>
      <p>{text}</p>
    </Modal>
  );
}
