package fpt.capstone.repository;

import fpt.capstone.entity.SupportItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportItemRepository extends JpaRepository<SupportItem, String> {

    @Query("""
            select si from SupportItem si
            where si.isDelete = false
              and (:q is null or lower(si.name) like lower(concat('%', :q, '%')))
            """)
    Page<SupportItem> search(@Param("q") String q, Pageable pageable);

    // Explicit JPQL: deriving an exists query from the boolean isDelete is rejected by Hibernate 7.
    @Query("select count(si) > 0 from SupportItem si where si.normalizedName = :normalizedName and si.isDelete = false")
    boolean existsActiveByNormalizedName(@Param("normalizedName") String normalizedName);
}
