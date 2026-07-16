package fpt.capstone.repository;

import fpt.capstone.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findById(int id);

    Optional<Role> findByName(String name);
}
