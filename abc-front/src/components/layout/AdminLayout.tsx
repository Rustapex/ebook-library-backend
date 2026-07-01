import { Link, Outlet } from 'react-router-dom';

const adminNavItems = [
  { to: '/admin', label: 'A-001 관리자 대시보드' },
  { to: '/admin/members', label: 'A-002 회원 목록 관리' },
  { to: '/admin/members/:memberId', label: 'A-003 회원 상세/상태 관리' },
  { to: '/admin/books', label: 'A-004 도서 목록 관리' },
  { to: '/admin/books/new', label: 'A-005 도서 등록' },
  { to: '/admin/books/:bookId/edit', label: 'A-006 도서 수정' },
  { to: '/admin/categories', label: 'A-007 카테고리 관리' },
  { to: '/admin/rentals', label: 'A-008 대여 현황 관리' },
  { to: '/admin/payments', label: 'A-009 결제 관리' },
  { to: '/admin/reports', label: 'A-010 신고 관리' },
  { to: '/admin/book-requests', label: 'A-011 희망도서 관리' },
  { to: '/admin/notices', label: 'A-012 공지 관리' },
  { to: '/admin/coupons-points', label: 'A-013 쿠폰/포인트 관리' },
  { to: '/admin/challenges', label: 'A-014 챌린지 관리' },
  { to: '/admin/libraries', label: 'A-015 도서관 위치 관리' },
  { to: '/admin/statistics', label: 'A-016 통계 관리' },
  { to: '/admin/audit-logs', label: 'A-017 관리자 감사 로그' },
  { to: '/admin/collections', label: 'A-018 컬렉션 관리' },
];

export function AdminLayout() {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <Link className="site-logo" to="/admin">
          ABC Admin
        </Link>
        <nav className="admin-nav" aria-label="Admin navigation">
          {adminNavItems.map((item) => (
            <Link key={item.to} to={item.to}>
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
