package fpt.capstone.repository;

import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenificiaryRepository extends JpaRepository<Benificiary, Integer> {
    List<Benificiary> findByApplication(int applicationId);

    List<Benificiary> findByApplicationIdAndDelete(int applicationId, boolean delete);

    Page<Benificiary> findAllByDelete(boolean delete, Pageable pageable);
}
