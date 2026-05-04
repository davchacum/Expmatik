import "../global-list.css";

const Modal = ({ children, onClose, style }) => {
  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="modal-box"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: "800px", ...style }}
      >
        {children}
      </div>
    </div>
  );
};

export default Modal;
