package fpt.capstone.repository;

import fpt.capstone.entity.SupportRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestAttachmentRepository extends JpaRepository<SupportRequestAttachment, String> {
    List<SupportRequestAttachment> findBySupportRequestId(String supportRequestId);
}