package com.acorn.abc.collection.repository;

import com.acorn.abc.collection.domain.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Page<Collection> findByStatus(String status, Pageable pageable);

    Page<Collection> findByStatusAndCollectionType(String status, String collectionType, Pageable pageable);
}
