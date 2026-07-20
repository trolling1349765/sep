package fpt.capstone.repository;

import fpt.capstone.entity.ItemAllocationPlan;
import fpt.capstone.enums.PlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemAllocationPlanRepository extends JpaRepository<ItemAllocationPlan, String> {

    /** List/search. DELETED plans are hidden unless status = DELETED is explicitly requested. */
    @Query("""
            select p from ItemAllocationPlan p
            where (:itemId is null or p.item.id = :itemId)
              and (:q is null or lower(p.code) like lower(concat('%', :q, '%')))
              and ((:status is null and p.status <> fpt.capstone.enums.PlanStatus.DELETED)
                   or p.status = :status)
            """)
    Page<ItemAllocationPlan> search(@Param("q") String q,
                                    @Param("itemId") String itemId,
                                    @Param("status") PlanStatus status,
                                    Pageable pageable);

    @Query("select p from ItemAllocationPlan p where p.item.id = :itemId and p.status = :status")
    List<ItemAllocationPlan> findByItemIdAndStatus(@Param("itemId") String itemId,
                                                   @Param("status") PlanStatus status);
}
