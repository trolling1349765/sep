package fpt.capstone.repository;

import fpt.capstone.entity.Chapter;
import fpt.capstone.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    Page<Chapter> findAllByDeleteFalseAndPolicyIdEquals(int policyId, Pageable pageable);
}
