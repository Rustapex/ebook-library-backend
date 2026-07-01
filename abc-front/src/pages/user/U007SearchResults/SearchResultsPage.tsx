import { EmptyState } from '../../../components/common/EmptyState';

export function SearchResultsPage() {
  return (
    <section className="page-section">
      <p className="eyebrow">U-007</p>
      <h1>도서 검색 결과</h1>
      <p>검색어 기반 도서 목록과 희망도서 신청 안내를 연결할 화면입니다.</p>
      <EmptyState title="검색 API 연결 전입니다." description="API-BOOK-002 연결 후 검색 결과를 표시합니다." />
    </section>
  );
}
