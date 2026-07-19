package fpt.capstone.repository;

import fpt.capstone.entity.Backup;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.enums.BackupType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Integer> {

    // Numeric max of the per-year sequence ("BK-2026-" is 8 chars, so the seq
    // starts at position 9). Casting avoids the lexicographic trap past 999.
    @Query("select max(cast(substring(b.code, 9) as integer)) from Backup b where b.code like :prefixPattern")
    Integer findMaxSequenceForPrefix(@Param("prefixPattern") String prefixPattern);

    // History listing: fixed sort (started_at desc, id desc tiebreaker),
    // callers pass an UNSORTED Pageable — same convention as SystemLogRepository.
    @Query(value = """
            select b from Backup b
            where (:status is null or b.status = :status)
              and (:type is null or b.type = :type)
            order by b.startedAt desc, b.id desc
            """,
            countQuery = """
            select count(b) from Backup b
            where (:status is null or b.status = :status)
              and (:type is null or b.type = :type)
            """)
    Page<Backup> search(@Param("status") BackupStatus status,
                        @Param("type") BackupType type,
                        Pageable pageable);

    Optional<Backup> findTopByOrderByStartedAtDescIdDesc();
}
