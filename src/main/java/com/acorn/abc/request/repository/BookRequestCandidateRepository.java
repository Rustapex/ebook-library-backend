package com.acorn.abc.request.repository;

import com.acorn.abc.request.domain.BookRequestCandidate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRequestCandidateRepository extends JpaRepository<BookRequestCandidate, Long> {

    List<BookRequestCandidate> findAllByOrderByFirstRequestedAtDesc(Pageable pageable);
}
