package com.acorn.abc.rental.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.repository.BookRentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final BookRentalRepository bookRentalRepository;

    // 뷰어/북마크 등 다른 도메인이 대여 소유권 확인이 필요할 때 이 메서드를 거치도록 한다
    public BookRental getOwnedRentalOrThrow(Long rentalId, Long memberId) {
        BookRental rental = bookRentalRepository.findById(rentalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RENTAL_NOT_FOUND));
        if (!rental.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        return rental;
    }
}
