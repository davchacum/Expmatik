import "./home.css";

const HomeMenu = () => {
  const items = [
    "Elemento 1",
    "Elemento 2",
    "Elemento 3",
    "Elemento 4",
    "Elemento 5",
    "Elemento 6",
    "Elemento 7",
    "Elemento 8",
    "Elemento 9",
  ];

  return (
    <div className="home-container">
      <div className="home-wrapper">
        <div className="menu-grid">
          {items.map((item, idx) => (
            <div key={idx} className="menu-item">
              {item}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default HomeMenu;
