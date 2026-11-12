export function Waiting() {
  return (
    <div className="overlay">
      <div className="modal glass waiting" role="status">
        <span className="status">
          <span className="spinner" />
          Oczekiwanie na połączenie z serwerem
        </span>
      </div>
    </div>
  );
}
