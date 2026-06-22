package fpt.capstone.repository;

import fpt.capstone.entity.Benificiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenificiaryRepository extends JpaRepository<Benificiary, Integer> {
}
