package fpt.capstone.repository;

import fpt.capstone.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

    // Recent-N for the admin dashboard (LIMIT via Pageable).
    List<SystemLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    // Audit-log search: fixed sort (created_at desc, id desc as tiebreaker for
    // stable pagination), callers pass an UNSORTED Pageable. Null-param guards
    // work on MySQL/Hibernate 6; if a future upgrade breaks the typed null
    // comparison, fall back to JpaSpecificationExecutor.
    @Query(value = """
            select l from SystemLog l
            where (:action is null or l.action = :action)
              and (:entityType is null or l.entityType = :entityType)
              and (:userId is null or l.userId = :userId)
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
            order by l.createdAt desc, l.id desc
            """,
            countQuery = """
            select count(l) from SystemLog l
            where (:action is null or l.action = :action)
              and (:entityType is null or l.entityType = :entityType)
              and (:userId is null or l.userId = :userId)
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
            """)
    Page<SystemLog> search(@Param("action") String action,
                           @Param("entityType") String entityType,
                           @Param("userId") String userId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           Pageable pageable);
}
