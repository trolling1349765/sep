package fpt.capstone.repository;

import fpt.capstone.entity.GoodsInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsInventoryRepository extends JpaRepository<GoodsInventory, Integer> {

    /** Pessimistic row lock by item — MUST be called inside a @Transactional stock mutation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GoodsInventory g where g.item.id = :itemId")
    Optional<GoodsInventory> lockByItemId(@Param("itemId") String itemId);

    Optional<GoodsInventory> findByItem_Id(String itemId);

    @Query("select g from GoodsInventory g where g.item.id in :itemIds and g.isDelete = false")
    List<GoodsInventory> findByItemIdIn(@Param("itemIds") Collection<String> itemIds);

    /**
     * Idempotently create the (0,0) balance row for an item before {@link #lockByItemId}.
     * INSERT IGNORE skips silently on the unique item_id — safe under concurrent first posts,
     * and (unlike catch-insert-retry) never poisons the surrounding transaction.
     */
    @Modifying
    @Query(value = "insert ignore into goods_inventories(item_id, quantity_on_hand, reserved_quantity, quantity, is_delete) "
            + "values (:itemId, 0, 0, 0, false)", nativeQuery = true)
    void ensureRow(@Param("itemId") String itemId);

    // Explicit JPQL: deriving from the boolean isDelete field is rejected by Hibernate 7.
    @Query("select g from GoodsInventory g where g.isDelete = false")
    Page<GoodsInventory> findByIsDeleteFalse(Pageable pageable);
}
