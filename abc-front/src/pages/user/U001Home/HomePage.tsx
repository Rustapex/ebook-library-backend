import { Link } from 'react-router-dom';

export function HomePage() {
  return (
    <section className="page-section hero-section">
      <p className="eyebrow">U-001</p>
      <h1>ABC 전자책 대여 서비스</h1>
      <p>메인 배너, 추천 도서, 카테고리, 공지 요약을 연결할 User 홈 화면입니다.</p>
      <Link className="button button-primary" to="/books">
        도서 둘러보기
      </Link>
    </section>
  );
}
