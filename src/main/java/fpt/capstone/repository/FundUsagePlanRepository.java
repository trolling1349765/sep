package fpt.capstone.repository;

import fpt.capstone.entity.FundUsagePlan;
import fpt.capstone.enums.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundUsagePlanRepository extends JpaRepository<FundUsagePlan, String> {

    /**
     * List/search. DELETED plans are hidden unless the caller explicitly filters
     * {@code status = DELETED} (soft-delete lives in the status, not isDelete).
     */
    @Query("""
            select p from FundUsagePlan p
            where (:donationId is null or p.donation.id = :donationId)
              and (:beneficiaryId is null or p.beneficiary.id = :beneficiaryId)
              and (:q is null
                   or lower(p.code) like lower(concat('%', :q, '%'))
                   or lower(p.purpose) like lower(concat('%', :q, '%')))
              and ((:status is null and p.status <> fpt.capstone.enums.PlanStatus.DELETED)
                   or p.status = :status)
            """)
    Page<FundUsagePlan> search(@Param("q") String q,
                               @Param("donationId") Integer donationId,
                               @Param("status") PlanStatus status,
                               @Param("beneficiaryId") Integer beneficiaryId,
                               Pageable pageable);

    @Query("select p from FundUsagePlan p where p.donation.id = :donationId")
    List<FundUsagePlan> findByDonationId(@Param("donationId") int donationId);

    @Query("select p from FundUsagePlan p where p.donation.id = :donationId and p.status = :status")
    List<FundUsagePlan> findByDonationIdAndStatus(@Param("donationId") int donationId,
                                                  @Param("status") PlanStatus status);
}
