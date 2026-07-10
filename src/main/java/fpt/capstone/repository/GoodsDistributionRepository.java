package fpt.capstone.repository;

import fpt.capstone.entity.GoodsDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsDistributionRepository extends JpaRepository<GoodsDistribution, Integer> {
    List<GoodsDistribution> findByAllocationPlanId(Integer allocationPlanId);

    List<GoodsDistribution> findByGoodsInventoryId(Integer goodsInventoryId);

    List<GoodsDistribution> findByBenificiaryId(Integer benificiaryId);
}