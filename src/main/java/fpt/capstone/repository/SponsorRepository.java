package fpt.capstone.repository;

import fpt.capstone.entity.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, String> {
    List<Sponsor> findByNameContainingIgnoreCase(String name);

    List<Sponsor> findBySponsorType(String sponsorType);

    List<Sponsor> findByStatus(String status);
}