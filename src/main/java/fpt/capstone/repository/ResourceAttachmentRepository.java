package fpt.capstone.repository;

import fpt.capstone.entity.ResourceAttachment;
import fpt.capstone.enums.AttachmentKind;
import fpt.capstone.enums.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceAttachmentRepository extends JpaRepository<ResourceAttachment, String> {

    List<ResourceAttachment> findByOwnerTypeAndOwnerIdOrderByUploadedAtDesc(Table ownerType, String ownerId);

    List<ResourceAttachment> findByOwnerTypeAndOwnerIdAndKindOrderByUploadedAtDesc(
            Table ownerType, String ownerId, AttachmentKind kind);
}
