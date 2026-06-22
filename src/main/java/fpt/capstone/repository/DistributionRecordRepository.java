package fpt.capstone.repository;

import fpt.capstone.entity.DistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Integer> {
}
