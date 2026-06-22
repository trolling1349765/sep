package fpt.capstone.repository;

import fpt.capstone.entity.EligibilityCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EligibilityCriteriaRepository extends JpaRepository<EligibilityCriteria, Integer> {
}
