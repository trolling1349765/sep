package fpt.capstone.repository;

import fpt.capstone.entity.DistributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionRecordRepository extends JpaRepository<DistributionRecord, Integer> {
    List<DistributionRecord> findByBenificiaryId(Integer benificiaryId);
}