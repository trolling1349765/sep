package fpt.capstone.integration;

import fpt.capstone.entity.Donation;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.repository.SupportItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link fpt.capstone.config.DonorDataSeeder} demo data loaded and that the
 * two module invariants hold on the seeded (and any test-created) rows:
 * money available = amount - pending - spent ≥ 0, stock available = onHand - reserved ≥ 0.
 */
class DonorSeedInvariantIT extends AbstractIntegrationTest {

    @Autowired private SponsorRepository sponsorRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private SupportItemRepository supportItemRepository;
    @Autowired private GoodsInventoryRepository goodsInventoryRepository;

    @Test
    void seed_loaded_andInvariantsHold() {
        // demo data present
        assertTrue(sponsorRepository.count() >= 2, "expected seeded sponsors");
        assertTrue(supportItemRepository.count() >= 2, "expected seeded items");

        // money invariant + a CONFIRMED funding with nothing allocated
        boolean confirmedUnallocated = false;
        for (Donation d : donationRepository.findAll()) {
            BigDecimal amount = nz(d.getAmount());
            BigDecimal pending = nz(d.getPendingAmount());
            BigDecimal spent = nz(d.getSpentAmount());
            BigDecimal available = amount.subtract(pending).subtract(spent);
            assertTrue(available.signum() >= 0, "money available must be >= 0 for donation " + d.getId());
            assertTrue(pending.signum() >= 0 && spent.signum() >= 0, "pending/spent must be >= 0");
            if (d.getStatus() == FundingStatus.CONFIRMED
                    && pending.signum() == 0 && spent.signum() == 0
                    && amount.compareTo(available) == 0) {
                confirmedUnallocated = true;
            }
        }
        assertTrue(confirmedUnallocated, "expected a CONFIRMED funding with available == amount");

        // stock invariant
        for (GoodsInventory g : goodsInventoryRepository.findAll()) {
            assertTrue(g.getQuantityOnHand() - g.getReservedQuantity() >= 0,
                    "stock available must be >= 0 for item " + (g.getItem() != null ? g.getItem().getId() : null));
            assertTrue(g.getQuantityOnHand() >= 0 && g.getReservedQuantity() >= 0, "on-hand/reserved must be >= 0");
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
