package fpt.capstone.repository;

import fpt.capstone.entity.ItemAllocationPlanLine;
import fpt.capstone.enums.PlanLineStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemAllocationPlanLineRepository extends JpaRepository<ItemAllocationPlanLine, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ItemAllocationPlanLine l where l.id = :id")
    Optional<ItemAllocationPlanLine> lockById(@Param("id") String id);

    List<ItemAllocationPlanLine> findByPlanId(String planId);

    long countByPlanIdAndLineStatus(String planId, PlanLineStatus lineStatus);
}
