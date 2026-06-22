package fpt.capstone.repository;

import fpt.capstone.entity.BenefitRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenifitRuleRepository extends JpaRepository<BenefitRule, Integer> {
}
