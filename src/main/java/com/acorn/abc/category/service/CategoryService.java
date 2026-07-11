package com.acorn.abc.category.service;

import com.acorn.abc.book.domain.Category;
import com.acorn.abc.category.dto.CategoryResponse;
import com.acorn.abc.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// open-in-view=false 환경에서 category.parent(LAZY) 접근을 조회와 같은 세션에서 하기 위해 필요하다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private static final String ACTIVE = "ACTIVE";

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories(Boolean includeChildren) {
        List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc(ACTIVE);
        if (!Boolean.TRUE.equals(includeChildren)) {
            return categories.stream()
                    .map(category -> new CategoryResponse(
                            category.getCategoryId(),
                            parentId(category),
                            category.getCategoryName(),
                            category.getDisplayOrder(),
                            List.of()
                    ))
                    .toList();
        }
        return toTree(categories);
    }

    private List<CategoryResponse> toTree(List<Category> categories) {
        Map<Long, MutableCategory> byId = new LinkedHashMap<>();
        for (Category category : categories) {
            byId.put(category.getCategoryId(), new MutableCategory(category));
        }

        List<MutableCategory> roots = new ArrayList<>();
        for (MutableCategory node : byId.values()) {
            Long parentId = parentId(node.value);
            MutableCategory parent = parentId == null ? null : byId.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }

        return roots.stream().map(MutableCategory::toResponse).toList();
    }

    private Long parentId(Category category) {
        return category.getParent() == null ? null : category.getParent().getCategoryId();
    }

    private static class MutableCategory {
        private final Category value;
        private final List<MutableCategory> children = new ArrayList<>();

        private MutableCategory(Category value) {
            this.value = value;
        }

        private CategoryResponse toResponse() {
            return new CategoryResponse(
                    value.getCategoryId(),
                    value.getParent() == null ? null : value.getParent().getCategoryId(),
                    value.getCategoryName(),
                    value.getDisplayOrder(),
                    children.stream().map(MutableCategory::toResponse).toList()
            );
        }
    }
}
