package fpt.capstone.repository;

import fpt.capstone.entity.BenefitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenifitHistoryRepository extends JpaRepository<BenefitHistory, Integer> {
}
