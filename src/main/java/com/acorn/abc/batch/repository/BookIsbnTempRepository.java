package com.acorn.abc.batch.repository;

import com.acorn.abc.batch.domain.BookIsbnTemp;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookIsbnTempRepository extends JpaRepository<BookIsbnTemp, Long> {

    Optional<BookIsbnTemp> findByIsbn13(String isbn13);

    Page<BookIsbnTemp> findByStatusCd(byte statusCd, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BookIsbnTemp t WHERE t.id IN :tempIds ORDER BY t.id ASC")
    List<BookIsbnTemp> findAllByIdInForUpdate(@Param("tempIds") List<Long> tempIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BookIsbnTemp t WHERE t.id IN :tempIds AND t.statusCd = :statusCd")
    int deleteAllByIdInAndStatusCd(@Param("tempIds") List<Long> tempIds,
                                   @Param("statusCd") byte statusCd);
}
