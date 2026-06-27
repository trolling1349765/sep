package fpt.capstone.repository;

import fpt.capstone.entity.SupportReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportReplyRepository extends JpaRepository<SupportReply, String> {
    List<SupportReply> findBySupportRequestIdOrderByCreatedAtAsc(String supportRequestId);
}