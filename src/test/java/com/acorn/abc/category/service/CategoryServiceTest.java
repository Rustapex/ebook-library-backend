package com.acorn.abc.category.service;

import com.acorn.abc.book.domain.Category;
import com.acorn.abc.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

// 15(국내도서) -> 158(소설/시/희곡) -> 159(한국소설) -> 160(2000년대 이후 한국소설) 4단계 트리 기준 검증.
// BookSpecifications.hasParentCategory가 직계 자식만 봐서 parentCategoryId=158/15로는 160에 매핑된
// 책을 못 찾던 문제(부모 카테고리 필터 버그) 수정을 위해 추가한 재귀 하위 카테고리 조회 테스트.
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findDescendantIds_다단계_트리에서_자기자신과_모든_하위_categoryId를_반환한다() {
        Category root = mockCategory(15L, null);
        Category depth1 = mockCategory(158L, root);
        Category depth2 = mockCategory(159L, depth1);
        Category leaf = mockCategory(160L, depth2);
        given(categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE"))
                .willReturn(List.of(root, depth1, depth2, leaf));

        assertThat(categoryService.findDescendantIds(15L)).containsExactlyInAnyOrder(15L, 158L, 159L, 160L);
        assertThat(categoryService.findDescendantIds(158L)).containsExactlyInAnyOrder(158L, 159L, 160L);
        assertThat(categoryService.findDescendantIds(159L)).containsExactlyInAnyOrder(159L, 160L);
        assertThat(categoryService.findDescendantIds(160L)).containsExactly(160L);
    }

    @Test
    void findDescendantIds_존재하지_않는_카테고리면_빈_리스트를_반환한다() {
        given(categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc("ACTIVE")).willReturn(List.of());

        assertThat(categoryService.findDescendantIds(999L)).isEmpty();
    }

    @Test
    void findDescendantIds_categoryId가_null이면_리포지토리_호출없이_빈_리스트를_반환한다() {
        assertThat(categoryService.findDescendantIds(null)).isEmpty();
        verifyNoInteractions(categoryRepository);
    }

    private Category mockCategory(Long id, Category parent) {
        Category category = mock(Category.class);
        given(category.getCategoryId()).willReturn(id);
        given(category.getParent()).willReturn(parent);
        return category;
    }
}
