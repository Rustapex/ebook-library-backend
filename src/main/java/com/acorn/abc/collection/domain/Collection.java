package com.acorn.abc.collection.domain;

import com.acorn.abc.common.domain.BaseEntity;
import com.acorn.abc.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "collection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Collection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collection_id")
    private Long collectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "collection_type", nullable = false, length = 20)
    private String collectionType;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // 관리자 컬렉션 API 구현계획(2026-07-16) §5.3 — 생성 시 관리자만 createdBy로 기록하고,
    // 이후 수정으로는 createdBy를 바꾸지 않는다.
    public static Collection create(String name, String collectionType, BigDecimal discountRate,
                                     String description, LocalDate startDate, LocalDate endDate,
                                     String status, Member createdBy) {
        Collection collection = new Collection();
        collection.name = name;
        collection.collectionType = collectionType;
        collection.discountRate = discountRate;
        collection.description = description;
        collection.startDate = startDate;
        collection.endDate = endDate;
        collection.status = status;
        collection.createdBy = createdBy;
        return collection;
    }

    // API-ADMIN-COLLECTION-002(PUT 수정) 전용 — collectionType 변경 가능 여부(mapping 존재 시 금지)는
    // Service에서 이 메서드 호출 전에 먼저 검증한다(D-08).
    public void updateBasicInfo(String name, String collectionType, BigDecimal discountRate,
                                 String description, LocalDate startDate, LocalDate endDate, String status) {
        this.name = name;
        this.collectionType = collectionType;
        this.discountRate = discountRate;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    // API-ADMIN-COLLECTION-004(PATCH) 전용 — status만 변경한다.
    public void changeStatus(String status) {
        this.status = status;
    }
}
