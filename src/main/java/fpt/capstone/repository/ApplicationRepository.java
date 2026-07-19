package fpt.capstone.repository;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    Page<Application> findAll( Pageable pageable);

    Page<Application> findBySubmitBy_Id(String submitBy, Pageable pageable);

    // Explicit JPQL: the JPA attribute is `isDelete` (Lombok boolean field), but the
    // derived `...Delete` name generated `a.delete`, which Hibernate 7 rejects at runtime.
    @Query("select a from Application a where a.isDelete = :delete")
    Page<Application> findByDelete(@Param("delete") boolean delete, Pageable pageable);

    @Query("select a from Application a where a.submitBy.id = :submitById and a.isDelete = :delete")
    Page<Application> findBySubmitBy_IdAndDelete(@Param("submitById") String submitById,
            @Param("delete") boolean delete, Pageable pageable);
}
