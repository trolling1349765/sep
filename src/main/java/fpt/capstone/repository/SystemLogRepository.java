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
    // :q is a pre-lowercased %...% pattern; the service escapes %/_/! with '!'
    // ('!' as the LIKE escape char sidesteps MySQL's backslash-in-literal
    // handling). The left join targets the users PK so rows never multiply.
    // Severity is derived from action, so the filter arrives as sevMode
    // IN/NOT_IN + an action list; sevActions must never be null (Hibernate
    // can't expand a null list) — the service passes a dummy single-element
    // list when sevMode is NONE.
    @Query(value = """
            select l from SystemLog l
              left join User u on u.id = l.userId
            where (:action is null or l.action = :action)
              and (:entityType is null or l.entityType = :entityType)
              and (:userId is null or l.userId = :userId)
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
              and (:q is null
                   or lower(l.action) like :q escape '!'
                   or lower(l.entityType) like :q escape '!'
                   or lower(l.entityId) like :q escape '!'
                   or lower(l.oldValue) like :q escape '!'
                   or lower(l.newValue) like :q escape '!'
                   or lower(u.name) like :q escape '!')
              and (:sevMode = 'NONE'
                   or (:sevMode = 'IN' and l.action in :sevActions)
                   or (:sevMode = 'NOT_IN' and l.action not in :sevActions))
            order by l.createdAt desc, l.id desc
            """,
            countQuery = """
            select count(l) from SystemLog l
              left join User u on u.id = l.userId
            where (:action is null or l.action = :action)
              and (:entityType is null or l.entityType = :entityType)
              and (:userId is null or l.userId = :userId)
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
              and (:q is null
                   or lower(l.action) like :q escape '!'
                   or lower(l.entityType) like :q escape '!'
                   or lower(l.entityId) like :q escape '!'
                   or lower(l.oldValue) like :q escape '!'
                   or lower(l.newValue) like :q escape '!'
                   or lower(u.name) like :q escape '!')
              and (:sevMode = 'NONE'
                   or (:sevMode = 'IN' and l.action in :sevActions)
                   or (:sevMode = 'NOT_IN' and l.action not in :sevActions))
            """)
    Page<SystemLog> search(@Param("action") String action,
                           @Param("entityType") String entityType,
                           @Param("userId") String userId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           @Param("q") String q,
                           @Param("sevMode") String sevMode,
                           @Param("sevActions") List<String> sevActions,
                           Pageable pageable);
}
