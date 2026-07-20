package fpt.capstone.repository;

import fpt.capstone.entity.Distribution;
import fpt.capstone.enums.DistributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DistributionRepository extends JpaRepository<Distribution, String> {

    /** Distribution history (5.7.14) — read-only, append-only. */
    @Query("""
            select d from Distribution d
            where (:itemId is null or d.planLine.plan.item.id = :itemId)
              and (:beneficiaryId is null or d.beneficiary.id = :beneficiaryId)
              and (:planId is null or d.planLine.plan.id = :planId)
              and (:status is null or d.status = :status)
              and (:fromDate is null or d.issueDate >= :fromDate)
              and (:toDate is null or d.issueDate <= :toDate)
            """)
    Page<Distribution> search(@Param("itemId") String itemId,
                              @Param("beneficiaryId") Integer beneficiaryId,
                              @Param("planId") String planId,
                              @Param("status") DistributionStatus status,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate,
                              Pageable pageable);

    /** Distributions for an item (via plan line) — the "−" entries of the inventory ledger. */
    @Query("select d from Distribution d where d.planLine.plan.item.id = :itemId")
    List<Distribution> findByItemId(@Param("itemId") String itemId);
}
