package com.acorn.abc.collection.controller;

import com.acorn.abc.collection.dto.CollectionDetailResponse;
import com.acorn.abc.collection.dto.CollectionSummaryResponse;
import com.acorn.abc.collection.service.CollectionService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Collections", description = "Collection public APIs")
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "Get collections", description = "Returns active SERIES or EVENT collections.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CollectionSummaryResponse>>> getCollections(
            @RequestParam(required = false) String collectionType,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer previewSize
    ) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getCollections(
                collectionType,
                status,
                page,
                size,
                previewSize
        )));
    }

    @Operation(summary = "Get collection detail", description = "Returns an active collection and available books in it.")
    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionDetailResponse>> getCollectionDetail(@PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.ok(collectionService.getCollectionDetail(collectionId)));
    }
}
