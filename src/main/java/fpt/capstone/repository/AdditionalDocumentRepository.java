package fpt.capstone.repository;

import fpt.capstone.entity.AdditionalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalDocumentRepository extends JpaRepository<AdditionalDocument, Integer> {
    List<AdditionalDocument> findByApplication(Integer applicationId);
}
