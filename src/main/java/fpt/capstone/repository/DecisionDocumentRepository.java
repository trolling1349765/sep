package fpt.capstone.repository;

import fpt.capstone.entity.DecisionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionDocumentRepository extends JpaRepository<DecisionDocument, Integer> {
}
