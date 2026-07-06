package fpt.capstone.repository;

import fpt.capstone.entity.WoundedSoldiers;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WounderSoldierRepository extends JpaRepository<WoundedSoldiers, Integer> {
}
