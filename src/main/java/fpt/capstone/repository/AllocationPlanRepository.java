package fpt.capstone.repository;

import fpt.capstone.entity.AllocationPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationPlanRepository extends JpaRepository<AllocationPlan, Integer> {
    List<AllocationPlan> findByStatus(String status);
}