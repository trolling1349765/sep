package fpt.capstone.repository;

import fpt.capstone.entity.Right;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RightRepository extends JpaRepository<Right, Integer> {

    Optional<Right> findByCode(String code);

    boolean existsByCode(String code);

    List<Right> findAllByOrderByModuleAscSortOrderAsc();
}
