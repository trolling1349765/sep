package fpt.capstone.repository;

import fpt.capstone.entity.FundAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundAllocationRepository extends JpaRepository<FundAllocation, Integer> {
    List<FundAllocation> findByDonationId(Integer donationId);
}