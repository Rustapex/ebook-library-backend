import { EmptyState } from '../../../components/common/EmptyState';

export function BooksPage() {
  return (
    <section className="page-section">
      <p className="eyebrow">U-006</p>
      <h1>도서 목록</h1>
      <p>카테고리, 무료/유료 필터와 도서 카드 목록을 구현할 화면입니다.</p>
      <EmptyState title="도서 API 연결 전입니다." description="API-BOOK-001 연결 후 BookCard 목록을 표시합니다." />
    </section>
  );
}
