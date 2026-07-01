import { Button } from '../../../components/common/Button';

export function SignupPage() {
  return (
    <section className="page-section form-page">
      <p className="eyebrow">U-002</p>
      <h1>회원가입</h1>
      <form className="form-card">
        <label>
          아이디
          <input name="loginId" type="text" />
        </label>
        <label>
          이름
          <input name="name" type="text" />
        </label>
        <label>
          이메일
          <input name="email" type="email" />
        </label>
        <label>
          생년월일
          <input name="birthDate" type="date" />
        </label>
        <Button type="button">회원가입 API 연결 예정</Button>
      </form>
    </section>
  );
}
