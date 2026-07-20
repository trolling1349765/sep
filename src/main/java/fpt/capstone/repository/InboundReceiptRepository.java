package fpt.capstone.repository;

import fpt.capstone.entity.InboundReceipt;
import fpt.capstone.enums.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, String> {

    @Query("""
            select r from InboundReceipt r
            where r.isDelete = false
              and (:q is null or lower(r.code) like lower(concat('%', :q, '%')))
              and (:status is null or r.status = :status)
              and (:itemId is null or r.item.id = :itemId)
            """)
    Page<InboundReceipt> search(@Param("q") String q,
                                @Param("status") ReceiptStatus status,
                                @Param("itemId") String itemId,
                                Pageable pageable);

    /** POSTED receipts for an item — the "+" entries of the inventory ledger. */
    @Query("select r from InboundReceipt r where r.item.id = :itemId and r.status = :status")
    List<InboundReceipt> findByItem_IdAndStatus(@Param("itemId") String itemId,
                                                @Param("status") ReceiptStatus status);

    /** Receipts tied to a sponsor — feeds the sponsor contribution-history tab (ITEM side). */
    @Query("select r from InboundReceipt r where r.sponsor.id = :sponsorId and r.isDelete = false")
    List<InboundReceipt> findBySponsorIdAndIsDeleteFalse(@Param("sponsorId") String sponsorId);
}
