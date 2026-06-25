package fpt.capstone.repository;

import fpt.capstone.entity.SupportRequest;
import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, String> {

    Page<SupportRequest> findByCitizenIdOrderByCreateAtDesc(String citizenId, Pageable pageable);

    @Query("SELECT sr FROM SupportRequest sr WHERE " +
            "(:status IS NULL OR sr.status = :status) AND " +
            "(:category IS NULL OR sr.category = :category) AND " +
            "(:dateFrom IS NULL OR sr.createAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR sr.createAt <= :dateTo) " +
            "ORDER BY sr.createAt DESC")
    Page<SupportRequest> findAllWithFilters(
            @Param("status") SupportRequestStatus status,
            @Param("category") SupportCategory category,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);

    boolean existsByReferenceNumber(String referenceNumber);
}