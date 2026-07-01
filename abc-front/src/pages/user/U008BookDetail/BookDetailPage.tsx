import { useParams } from 'react-router-dom';

export function BookDetailPage() {
  const { bookId } = useParams();

  return (
    <section className="page-section">
      <p className="eyebrow">U-008</p>
      <h1>도서 상세</h1>
      <p>선택한 도서 ID: {bookId ?? '미지정'}</p>
      <p>도서 정보, 대여, 리뷰, 즐겨찾기, 신고 모달, 추천 도서를 연결할 화면입니다.</p>
    </section>
  );
}
