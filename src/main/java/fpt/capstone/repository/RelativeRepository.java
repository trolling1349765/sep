package fpt.capstone.repository;

import fpt.capstone.dto.response.RelativeResponse;
import fpt.capstone.entity.Relative;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelativeRepository extends JpaRepository<Relative, Integer> {
    Relative findByApplication(int applicationId);
}
