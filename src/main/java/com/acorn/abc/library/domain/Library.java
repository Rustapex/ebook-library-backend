package com.acorn.abc.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "library")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_id")
    private Long libraryId;

    @Column(name = "library_name", nullable = false, length = 100)
    private String libraryName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "library_code", length = 20)
    private String libraryCode;

    @Column(name = "region_code", length = 2)
    private String regionCode;

    @Column(name = "dtl_region_code", length = 5)
    private String dtlRegionCode;

    @Column(name = "source", nullable = false, length = 20)
    private String source;
}
