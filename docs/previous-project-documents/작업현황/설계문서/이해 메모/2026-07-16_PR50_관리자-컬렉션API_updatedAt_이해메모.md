# PR50 관리자 컬렉션 API updatedAt 이해메모

API: `PUT /api/v1/admin/collections/{collectionId}`  
목적: 수정 성공 응답의 `updatedAt`을 실제 DB 저장값과 일치시킨다.  
흐름: 잠금 조회 → 엔티티 정보 변경 → 감사 로그 기록 → repository flush → 응답 DTO 생성.  
DB: `collection.updated_at`은 runtime `schema.sql`에 존재하며 `BaseEntity.@LastModifiedDate`가 관리한다.  
주의: 공개 EVENT 조회는 3차 재검증에서 정상이며, ErrorCode·전역 Handler·스키마는 변경하지 않는다.
