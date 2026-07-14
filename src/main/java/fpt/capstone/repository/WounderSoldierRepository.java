package fpt.capstone.repository;

import fpt.capstone.entity.WoundedSoldiers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WounderSoldierRepository extends JpaRepository<WoundedSoldiers, Integer> {
    List<WoundedSoldiers> findByBenificiary(int benificiaryId);
}
