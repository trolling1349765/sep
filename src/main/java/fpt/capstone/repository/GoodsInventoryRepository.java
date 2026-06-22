package fpt.capstone.repository;

import fpt.capstone.entity.GoodsInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsInventoryRepository extends JpaRepository<GoodsInventory, Integer> {
}
