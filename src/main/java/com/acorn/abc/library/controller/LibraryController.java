package com.acorn.abc.library.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.library.dto.LibrarySummaryResponse;
import com.acorn.abc.library.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Libraries", description = "Library public APIs")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @Operation(summary = "Get libraries holding a book", description = "Returns active libraries that hold the given book.")
    @GetMapping("/{bookId}/libraries")
    public ResponseEntity<ApiResponse<List<LibrarySummaryResponse>>> getLibraries(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.ok(libraryService.getLibrariesByBookId(bookId)));
    }
}
