import { Button } from '../../../components/common/Button';

export function LoginPage() {
  return (
    <section className="page-section form-page">
      <p className="eyebrow">U-003</p>
      <h1>로그인</h1>
      <form className="form-card">
        <label>
          아이디
          <input name="loginId" type="text" placeholder="아이디를 입력하세요" />
        </label>
        <label>
          비밀번호
          <input name="password" type="password" placeholder="비밀번호를 입력하세요" />
        </label>
        <Button type="button">로그인 API 연결 예정</Button>
      </form>
    </section>
  );
}
