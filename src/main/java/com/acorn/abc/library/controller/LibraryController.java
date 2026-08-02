package com.acorn.abc.library.controller;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.library.dto.LibrarySearchResponse;
import com.acorn.abc.library.service.ClientIpResolver;
import com.acorn.abc.library.service.LibraryRateLimiter;
import com.acorn.abc.library.service.PhysicalLibrarySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// GET /api/v1/books/{bookId}/libraries: 종이책 소장 도서관 조회(계획 4장). region은 필수이며 미전달 시
// MissingServletRequestParameterException으로 400 VALIDATION_ERROR가 발생한다. 기존 내부 DB 전용
// 조회(무-query)는 소비자가 없음을 확인해 즉시 새 계약으로 교체했다(계획 4.1).
@Tag(name = "Libraries", description = "Library public APIs")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Validated
public class LibraryController {

    private final PhysicalLibrarySearchService physicalLibrarySearchService;
    private final LibraryRateLimiter libraryRateLimiter;
    private final ClientIpResolver clientIpResolver;

    @Operation(summary = "종이책 소장 도서관 조회",
            description = "region은 필수이며, 좌표는 지도 중심 이동과 현재 page 내부 거리 표시에만 사용된다.")
    @GetMapping("/{bookId}/libraries")
    public ResponseEntity<ApiResponse<LibrarySearchResponse>> getLibraries(
            @PathVariable Long bookId,
            @RequestParam @NotBlank @Pattern(regexp = "^[0-9]{2}$") String region,
            @RequestParam(required = false) @Pattern(regexp = "^[0-9]{5}$") String dtlRegion,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        String clientIp = clientIpResolver.resolve(request);
        if (!libraryRateLimiter.tryConsume(clientIp)) {
            throw new BusinessException(ErrorCode.LIBRARY_RATE_LIMIT_EXCEEDED);
        }
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        LibrarySearchResponse response = physicalLibrarySearchService.search(
                bookId, region, dtlRegion, page, size, latitude, longitude);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
