package com.acorn.abc.category.service;

import com.acorn.abc.book.domain.Category;
import com.acorn.abc.category.dto.CategoryResponse;
import com.acorn.abc.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    // parentCategoryId로 도서를 필터링할 때, 직계 자식만이 아니라 모든 하위(자식의 자식...) 카테고리를
    // 포함해야 하므로 자기 자신 + 전체 하위 categoryId를 BFS로 모아 반환한다.
    // 존재하지 않거나 비활성 카테고리면 빈 리스트를 반환한다(호출 측에서 "매칭 결과 없음"으로 처리).
    public List<Long> findDescendantIds(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        List<Category> categories = categoryRepository.findByStatusOrderByDisplayOrderAscCategoryIdAsc(ACTIVE);

        Map<Long, List<Long>> childIdsByParentId = new LinkedHashMap<>();
        boolean exists = false;
        for (Category category : categories) {
            if (categoryId.equals(category.getCategoryId())) {
                exists = true;
            }
            Long parentId = parentId(category);
            if (parentId != null) {
                childIdsByParentId.computeIfAbsent(parentId, id -> new ArrayList<>()).add(category.getCategoryId());
            }
        }
        if (!exists) {
            return List.of();
        }

        List<Long> descendantIds = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            descendantIds.add(current);
            queue.addAll(childIdsByParentId.getOrDefault(current, List.of()));
        }
        return descendantIds;
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
