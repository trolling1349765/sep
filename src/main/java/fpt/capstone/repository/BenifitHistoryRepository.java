package fpt.capstone.repository;

import fpt.capstone.entity.BenefitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenifitHistoryRepository extends JpaRepository<BenefitHistory, Integer> {
    Page<BenefitHistory> findByBenificiary(Integer id, Pageable pageable);

    Page<BenefitHistory> findAllByDeleteIsFalse(boolean delete, Pageable pageable);
}
