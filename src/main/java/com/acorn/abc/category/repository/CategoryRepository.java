package com.acorn.abc.category.repository;

import com.acorn.abc.book.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStatusOrderByDisplayOrderAscCategoryIdAsc(String status);
}
