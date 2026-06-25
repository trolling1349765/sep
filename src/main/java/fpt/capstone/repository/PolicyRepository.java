package fpt.capstone.repository;

import fpt.capstone.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> {

    @Query("SELECT p FROM Policy p WHERE " +
            "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.documentNo) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR p.documentType = :category) " +
            "AND p.isDelete = false " +
            "ORDER BY p.issuedDate DESC")
    Page<Policy> searchPolicies(@Param("keyword") String keyword,
            @Param("category") String category,
            Pageable pageable);

    @Query("SELECT DISTINCT p.documentType FROM Policy p WHERE p.isDelete = false AND p.documentType IS NOT NULL")
    List<String> findDistinctDocumentTypes();

    @Query("SELECT p.documentType, COUNT(p) FROM Policy p WHERE p.isDelete = false GROUP BY p.documentType")
    List<Object[]> countByDocumentType();
}
