package fpt.capstone.repository;

import fpt.capstone.entity.GoodsInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsInventoryRepository extends JpaRepository<GoodsInventory, Integer> {
    List<GoodsInventory> findByItemNameContainingIgnoreCase(String itemName);

    List<GoodsInventory> findBySponsorId(String sponsorId);

    List<GoodsInventory> findByStatus(String status);
}