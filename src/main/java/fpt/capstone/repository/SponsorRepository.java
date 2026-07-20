package fpt.capstone.repository;

import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, String> {

    @Query("""
            select s from Sponsor s
            where s.isDelete = false
              and (:q is null
                   or lower(s.name) like lower(concat('%', :q, '%'))
                   or lower(s.code) like lower(concat('%', :q, '%'))
                   or lower(s.contactPerson) like lower(concat('%', :q, '%'))
                   or lower(s.phone) like lower(concat('%', :q, '%')))
              and (:type is null or s.type = :type)
              and (:status is null or s.status = :status)
            """)
    Page<Sponsor> search(@Param("q") String q,
                         @Param("type") SponsorType type,
                         @Param("status") SponsorStatus status,
                         Pageable pageable);

    /**
     * BR-47 duplicate detection: another sponsor sharing the same orgCode (when given),
     * phone, or normalized name. {@code excludeId} is the record itself on update (null on create).
     */
    @Query("""
            select s from Sponsor s
            where s.isDelete = false
              and (:excludeId is null or s.id <> :excludeId)
              and ((:orgCode is not null and s.orgCode = :orgCode)
                   or s.phone = :phone
                   or s.normalizedName = :normalizedName)
            """)
    List<Sponsor> findDuplicates(@Param("orgCode") String orgCode,
                                 @Param("phone") String phone,
                                 @Param("normalizedName") String normalizedName,
                                 @Param("excludeId") String excludeId);

    @Query("select s from Sponsor s where s.isDelete = false and s.status = fpt.capstone.enums.SponsorStatus.ACTIVE")
    List<Sponsor> findAllActive();
}
