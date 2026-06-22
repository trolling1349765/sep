package fpt.capstone.repository;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
}
