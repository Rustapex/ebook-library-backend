package com.acorn.abc.category.controller;

import com.acorn.abc.category.dto.CategoryResponse;
import com.acorn.abc.category.service.CategoryService;
import com.acorn.abc.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories", description = "Category public APIs")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get categories", description = "Returns active categories sorted by display order.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "true") Boolean includeChildren
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getCategories(includeChildren)));
    }
}
