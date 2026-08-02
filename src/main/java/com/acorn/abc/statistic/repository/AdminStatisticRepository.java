package com.acorn.abc.statistic.repository;

import com.acorn.abc.statistic.domain.AdminStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AdminStatisticRepository extends JpaRepository<AdminStatistic, Long> {

    Optional<AdminStatistic> findByPeriodTypeAndPeriodStartDateAndAgeBand(
            String periodType, LocalDate periodStartDate, String ageBand);
}
