package com.acorn.abc.rental.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.domain.ReadingProgress;
import com.acorn.abc.rental.dto.AdminRentalPaymentSummaryResponse;
import com.acorn.abc.rental.dto.AdminRentalSummaryResponse;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 대여 현황 조회(API-ADMIN-RENTAL-001) 서비스. 조회 전용이며 상태 전이·감사 로그를 다루지 않는다(구현계획 §2.3).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRentalService {

    private static final Set<String> STATUS_VALUES = Set.of("READY", "READING", "OWNED");

    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final PaymentRepository paymentRepository;

    public PageResponse<AdminRentalSummaryResponse> getAdminRentals(String q, Long memberId, String status,
                                                                       int page, int size) {
        validatePaging(page, size);
        validateStatus(status);

        String likeQ = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Page<BookRental> result = bookRentalRepository.searchForAdmin(likeQ, memberId, status, PageRequest.of(page, size));

        List<Long> rentalIds = result.getContent().stream().map(BookRental::getRentalId).toList();

        Map<Long, ReadingProgress> progressByRentalId = rentalIds.isEmpty() ? Map.of()
                : readingProgressRepository.findByRental_RentalIdIn(rentalIds).stream()
                    .collect(Collectors.toMap(p -> p.getRental().getRentalId(), Function.identity()));

        Map<Long, Payment> paymentByRentalId = rentalIds.isEmpty() ? Map.of()
                : paymentRepository.findByPaymentStatusAndRental_RentalIdIn("PAID", rentalIds).stream()
                    .collect(Collectors.toMap(p -> p.getRental().getRentalId(), Function.identity(), (a, b) -> a));

        List<AdminRentalSummaryResponse> content = result.getContent().stream()
                .map(r -> toSummary(r, progressByRentalId.get(r.getRentalId()), paymentByRentalId.get(r.getRentalId())))
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    private AdminRentalSummaryResponse toSummary(BookRental rental, ReadingProgress progress, Payment payment) {
        AdminRentalPaymentSummaryResponse paymentSummary = payment == null ? null
                : new AdminRentalPaymentSummaryResponse(payment.getPaymentId(), payment.getAmount(),
                        payment.getPaymentMethod(), payment.getPaidAt());

        return new AdminRentalSummaryResponse(
                rental.getRentalId(),
                rental.getMember().getMemberId(),
                rental.getMember().getName(),
                rental.getBook().getBookId(),
                rental.getBook().getTitle(),
                rental.getRentalStatus(),
                rental.getCreatedAt(),
                rental.getRentalStartAt(),
                rental.getRentalEndAt(),
                progress == null ? null : progress.getProgression(),
                paymentSummary
        );
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateStatus(String status) {
        if (status != null && !STATUS_VALUES.contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
