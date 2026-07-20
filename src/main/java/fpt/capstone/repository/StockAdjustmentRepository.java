package fpt.capstone.repository;

import fpt.capstone.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, String> {

    /** Adjustment history for an item — the "±" entries of the inventory ledger. */
    @Query("select a from StockAdjustment a where a.item.id = :itemId")
    List<StockAdjustment> findByItem_Id(@Param("itemId") String itemId);
}
