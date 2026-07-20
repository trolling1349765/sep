package fpt.capstone.config;

import fpt.capstone.entity.Donation;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.FundingStatus;
import fpt.capstone.enums.PaymentMethod;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds a small, invariant-safe demo dataset for the donor/resource module (Đợt 6 §4.4):
 * two sponsors, one CONFIRMED funding, two support items and their stock. Runs once on a
 * fresh DB (guarded by sponsor count) after {@link DataInitializer} (@Order). Deliberately
 * isolated and defensive — a seed failure logs and is swallowed so it can never block boot.
 * Plans/distributions are exercised by the integration tests, not seeded here.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DonorDataSeeder implements CommandLineRunner {

    private final SponsorRepository sponsorRepository;
    private final DonationRepository donationRepository;
    private final SupportItemRepository supportItemRepository;
    private final GoodsInventoryRepository goodsInventoryRepository;
    private final CodeGenerator codeGenerator;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        try {
            if (sponsorRepository.count() > 0) {
                return;
            }
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());
            log.info(">>> Donor demo data seeded.");
        } catch (Exception e) {
            log.warn("Donor demo seed skipped: {}", e.getMessage());
        }
    }

    private void seed() {
        Sponsor org = sponsorRepository.save(Sponsor.builder()
                .code(codeGenerator.next("NTT"))
                .name("Quỹ Thiện Nguyện Ánh Dương")
                .normalizedName(TextNormalizer.normalize("Quỹ Thiện Nguyện Ánh Dương"))
                .type(SponsorType.ORG).orgCode("0312345678")
                .contactPerson("Nguyễn Văn An").phone("0281234567")
                .email("anhduong@example.org").address("Hà Nội")
                .status(SponsorStatus.ACTIVE)
                .createAt(LocalDate.now()).createBy("system").build());

        sponsorRepository.save(Sponsor.builder()
                .code(codeGenerator.next("NTT"))
                .name("Bà Trần Thị Bình")
                .normalizedName(TextNormalizer.normalize("Bà Trần Thị Bình"))
                .type(SponsorType.INDIVIDUAL)
                .contactPerson("Trần Thị Bình").phone("0287654321")
                .email("binh@example.com").address("TP. Hồ Chí Minh")
                .status(SponsorStatus.ACTIVE)
                .createAt(LocalDate.now()).createBy("system").build());

        // One CONFIRMED funding of 100tr, no allocation yet -> available = amount.
        donationRepository.save(Donation.builder()
                .code(codeGenerator.next("KP"))
                .name("Ủng hộ đồng bào lũ lụt")
                .sponsor(org)
                .amount(new BigDecimal("100000000"))
                .pendingAmount(BigDecimal.ZERO)
                .spentAmount(BigDecimal.ZERO)
                .purpose("Hỗ trợ khẩn cấp")
                .paymentMethod(PaymentMethod.TRANSFER).transactionRef("FT26010112345")
                .evidenceName("Giấy chuyển khoản")
                .status(FundingStatus.CONFIRMED).recordedBy("system")
                .transferDate(LocalDate.of(2026, 1, 1))
                .createAt(LocalDate.now()).createBy("system").build());

        SupportItem rice = seedItem("Gạo tẻ", "kg");
        seedItem("Chăn ấm", "cái");
        // Stock for rice: 1000 on hand, nothing reserved.
        goodsInventoryRepository.save(GoodsInventory.builder()
                .item(rice).quantityOnHand(1000).reservedQuantity(0)
                .createAt(LocalDate.now()).createBy("system").build());
    }

    private SupportItem seedItem(String name, String unit) {
        return supportItemRepository.save(SupportItem.builder()
                .code(codeGenerator.next("VP"))
                .name(name).normalizedName(TextNormalizer.normalize(name)).unit(unit)
                .createAt(LocalDate.now()).createBy("system").build());
    }
}
