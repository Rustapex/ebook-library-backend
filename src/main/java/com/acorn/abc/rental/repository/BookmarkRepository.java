package com.acorn.abc.rental.repository;

import com.acorn.abc.rental.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByRental_RentalIdAndPageNo(Long rentalId, int pageNo);

    Optional<Bookmark> findByBookmarkIdAndRental_RentalId(Long bookmarkId, Long rentalId);

    List<Bookmark> findByRental_RentalIdOrderByPageNoAsc(Long rentalId);

    boolean existsByRental_RentalIdAndEpubVersionAndLocationHash(Long rentalId, int epubVersion, byte[] locationHash);

    List<Bookmark> findByRental_RentalIdAndEpubVersionOrderByProgressionAsc(Long rentalId, int epubVersion);

    long deleteByRental_Book_BookId(Long bookId);
}
