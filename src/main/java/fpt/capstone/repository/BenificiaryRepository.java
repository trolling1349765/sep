package fpt.capstone.repository;

import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenificiaryRepository extends JpaRepository<Benificiary, Integer> {
    List<Benificiary> findByApplication(int applicationId);

    // Explicit JPQL: the JPA attribute is `isDelete`, the derived `...Delete` name
    // generated `b.delete`, which Hibernate 7 rejects at runtime.
    @Query("select b from Benificiary b where b.application.id = :applicationId and b.isDelete = :delete")
    List<Benificiary> findByApplicationIdAndDelete(@Param("applicationId") int applicationId,
            @Param("delete") boolean delete);

    @Query("select b from Benificiary b where b.isDelete = :delete")
    Page<Benificiary> findAllByDelete(@Param("delete") boolean delete, Pageable pageable);
}
