import { Link, Outlet } from 'react-router-dom';

const userNavItems = [
  { to: '/', label: '홈' },
  { to: '/books', label: '도서' },
  { to: '/login', label: '로그인' },
  { to: '/signup', label: '회원가입' },
  { to: '/me', label: '마이페이지' },
];

export function UserLayout() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <Link className="site-logo" to="/">
          ABC
        </Link>
        <nav className="site-nav" aria-label="User navigation">
          {userNavItems.map((item) => (
            <Link key={item.to} to={item.to}>
              {item.label}
            </Link>
          ))}
        </nav>
      </header>
      <main className="site-main">
        <Outlet />
      </main>
      <footer className="site-footer">Acorn Book Cloud</footer>
    </div>
  );
}
