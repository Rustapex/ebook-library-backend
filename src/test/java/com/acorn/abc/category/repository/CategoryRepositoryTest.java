package com.acorn.abc.category.repository;

import com.acorn.abc.book.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        insertCategory(null, "Novel", 2, "ACTIVE");
        insertCategory(null, "Essay", 1, "ACTIVE");
        insertCategory(null, "Retired", 0, "INACTIVE");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsOnlyActiveCategoriesOrderedByDisplayOrder() {
        List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE");

        assertThat(categories).extracting(Category::getCategoryName).containsExactly("Essay", "Novel");
        assertThat(categories).extracting(Category::getStatus).allMatch("ACTIVE"::equals);
    }

    private void insertCategory(Long parentId, String name, int displayOrder, String status) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO category (parent_category_id, category_name, display_order, status) VALUES (?, ?, ?, ?)")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter(1, parentId)
                .setParameter(2, name)
                .setParameter(3, displayOrder)
                .setParameter(4, status)
                .executeUpdate();
    }
}
