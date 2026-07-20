package fpt.capstone.repository;

import fpt.capstone.entity.Donation;
import fpt.capstone.enums.FundingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Integer> {

    /** Pessimistic row lock — MUST be called inside a @Transactional balance mutation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> findByIdForUpdate(@Param("id") int id);

    /** Funding records tied to a sponsor — feeds the sponsor contribution-history tab. */
    @Query("select d from Donation d where d.isDelete = false and d.sponsor.id = :sponsorId")
    List<Donation> findBySponsorId(@Param("sponsorId") String sponsorId);

    @Query("""
            select d from Donation d
            where d.isDelete = false
              and (:status is null or d.status = :status)
              and (:sponsorId is null or d.sponsor.id = :sponsorId)
              and (:fromDate is null or d.transferDate >= :fromDate)
              and (:toDate is null or d.transferDate <= :toDate)
            """)
    Page<Donation> search(@Param("status") FundingStatus status,
                          @Param("sponsorId") String sponsorId,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate,
                          Pageable pageable);
}
