package fpt.capstone.config;

import fpt.capstone.entity.*;
import fpt.capstone.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

        private final PolicyRepository policyRepository;
        private final ArticleRepository articleRepository;
        private final EligibilityCriteriaRepository eligibilityCriteriaRepository;
        private final BenifitRuleRepository benefitRuleRepository;
        private final ChapterRepository chapterRepository;

        public DataInitializer(
                PolicyRepository policyRepository,
                ArticleRepository articleRepository,
                EligibilityCriteriaRepository eligibilityCriteriaRepository,
                BenifitRuleRepository benefitRuleRepository,
                ChapterRepository chapterRepository
        ) {
                this.policyRepository = policyRepository;
                this.articleRepository = articleRepository;
                this.eligibilityCriteriaRepository = eligibilityCriteriaRepository;
                this.benefitRuleRepository = benefitRuleRepository;
                this.chapterRepository = chapterRepository;
        }

        @Override
        @Transactional
        public void run(String... args) {
                if (policyRepository.count() > 0) {
                        // Data already exists, skip seeding
                        return;
                }

                // ===== Policy 1: Social Assistance for the Elderly =====
                Policy p1 = Policy.builder()
                                .documentNo("01/2024/ND-CP")
                                .title("Nghị định về trợ cấp xã hội đối với người cao tuổi")
                                .documentType("Nghị định")
                                .issuedDate(LocalDate.of(2024, 1, 15))
                                .effectiveDate(LocalDate.of(2024, 3, 1))
                                .expiredDate(null)
                                .issuer("Chính phủ")
                                .summary(
                                                "Nghị định này quy định về chế độ trợ cấp xã hội hàng tháng đối với người cao tuổi từ đủ 80 tuổi trở lên không có lương hưu hoặc trợ cấp bảo hiểm xã hội. Mức trợ cấp được tính dựa trên chuẩn nghèo và hệ số theo độ tuổi.")
                                .build();
                p1 = policyRepository.save(p1);
                Chapter c1 = Chapter.builder()
                        .policy(p1)
                        .title("chapter I")
                        .build();
                c1 = chapterRepository.save(c1);
                articleRepository.save(Article.builder()
                                .chapter(c1)
                                .articleNo(1)
                                .title("Phạm vi điều chỉnh")
                                .content(
                                                "Nghị định này quy định về đối tượng, mức hưởng và thủ tục thực hiện trợ cấp xã hội hàng tháng cho người cao tuổi.")
                                .build());
                articleRepository.save(Article.builder()
                                .chapter(c1).articleNo(2)
                                .title("Đối tượng áp dụng")
                                .content(
                                                "Công dân Việt Nam từ đủ 80 tuổi trở lên, không có lương hưu, không có trợ cấp bảo hiểm xã hội hàng tháng và thuộc hộ gia đình có hoàn cảnh khó khăn.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p1)
                                .applicableSubject("Người cao tuổi")
                                .conditionValue("Từ đủ 80 tuổi trở lên")
                                .benchmark("Không có lương hưu hoặc trợ cấp BHXH hàng tháng")
                                .build());
                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p1)
                                .applicableSubject("Hộ gia đình")
                                .conditionValue("Thuộc diện hộ nghèo hoặc cận nghèo")
                                .benchmark("Theo chuẩn nghèo đa chiều quốc gia")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p1)
                                .formula("Mức trợ cấp = Chuẩn nghèo × Hệ số tuổi")
                                .benchmark("Chuẩn nghèo: 1.500.000 VNĐ/tháng (2024)")
                                .multiplier(1.2)
                                .build());

                // ===== Policy 2: Support for Orphans and Abandoned Children =====
                Policy p2 = Policy.builder()
                                .documentNo("02/2024/ND-CP")
                                .title("Nghị định về hỗ trợ trẻ em mồ côi và trẻ em bị bỏ rơi")
                                .documentType("Nghị định")
                                .issuedDate(LocalDate.of(2024, 3, 20))
                                .effectiveDate(LocalDate.of(2024, 5, 1))
                                .expiredDate(null)
                                .issuer("Chính phủ")
                                .summary(
                                                "Quy định về chính sách hỗ trợ nuôi dưỡng, học tập và chăm sóc sức khỏe đối với trẻ em mồ côi cả cha lẫn mẹ, trẻ em bị bỏ rơi dưới 16 tuổi.")
                                .build();
                p2 = policyRepository.save(p2);
                Chapter c2 = Chapter.builder()
                        .policy(p2)
                        .title("chapter I")
                        .build();
                articleRepository.save(Article.builder()
                                .chapter(c2).articleNo(1)
                                .title("Đối tượng thụ hưởng")
                                .content(
                                                "Trẻ em dưới 16 tuổi mồ côi cả cha và mẹ, trẻ em bị bỏ rơi, trẻ em không nơi nương tựa đang được nuôi dưỡng tại các cơ sở bảo trợ xã hội.")
                                .build());
                articleRepository.save(Article.builder()
                                .chapter(c2).articleNo(2)
                                .title("Mức hỗ trợ")
                                .content(
                                                "Mức hỗ trợ nuôi dưỡng hàng tháng bằng 2,5 lần chuẩn trợ giúp xã hội. Hỗ trợ học phí và chi phí học tập theo quy định của pháp luật về giáo dục.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p2)
                                .applicableSubject("Trẻ em")
                                .conditionValue("Dưới 16 tuổi, mồ côi cả cha lẫn mẹ hoặc bị bỏ rơi")
                                .benchmark("Có xác nhận của UBND cấp xã hoặc cơ sở bảo trợ xã hội")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p2)
                                .formula("Mức hỗ trợ = Chuẩn trợ giúp xã hội × 2.5")
                                .benchmark("Chuẩn trợ giúp xã hội: 500.000 VNĐ/tháng")
                                .multiplier(2.5)
                                .build());

                // ===== Policy 3: Vocational Training Support for People with Disabilities
                // =====
                Policy p3 = Policy.builder()
                                .documentNo("03/2024/TT-BLDTBXH")
                                .title("Thông tư hướng dẫn hỗ trợ đào tạo nghề cho người khuyết tật")
                                .documentType("Thông tư")
                                .issuedDate(LocalDate.of(2024, 4, 10))
                                .effectiveDate(LocalDate.of(2024, 6, 1))
                                .expiredDate(null)
                                .issuer("Bộ Lao động - Thương binh và Xã hội")
                                .summary(
                                                "Thông tư này hướng dẫn thực hiện chính sách hỗ trợ đào tạo nghề và tạo việc làm cho người khuyết tật, bao gồm hỗ trợ học phí, chi phí sinh hoạt và hỗ trợ sau đào tạo.")
                                .build();
                p3 = policyRepository.save(p3);
                Chapter c3 = Chapter.builder()
                                .policy(p3)
                                .title("Chapter I")
                                .build();
                c3 = chapterRepository.save(c3);
                articleRepository.save(Article.builder()
                                .chapter(c3).articleNo(1)
                                .title("Nguyên tắc hỗ trợ")
                                .content(
                                                "Người khuyết tật được hỗ trợ đào tạo nghề phù hợp với khả năng và nhu cầu. Mức hỗ trợ tối đa không quá 12 tháng cho mỗi khóa học.")
                                .build());
                articleRepository.save(Article.builder()
                                .chapter(c3).articleNo(2)
                                .title("Mức hỗ trợ")
                                .content(
                                                "Hỗ trợ 100% học phí đào tạo nghề. Hỗ trợ tiền ăn hàng ngày bằng 0.5 mức lương cơ sở. Hỗ trợ phương tiện đi lại tối đa 300.000 VNĐ/tháng.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p3)
                                .applicableSubject("Người khuyết tật")
                                .conditionValue("Có giấy xác nhận khuyết tật theo quy định")
                                .benchmark("Độ tuổi từ 15 đến 55, có khả năng học nghề")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p3)
                                .formula("Hỗ trợ hàng tháng = Học phí + (0.5 × Lương cơ sở × Số ngày học) + Hỗ trợ đi lại")
                                .benchmark("Lương cơ sở: 1.800.000 VNĐ (2024)")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 4: Emergency Assistance for Natural Disaster Victims =====
                Policy p4 = Policy.builder()
                                .documentNo("04/2024/QD-TTg")
                                .title("Quyết định về chính sách hỗ trợ khẩn cấp cho người dân bị thiên tai")
                                .documentType("Quyết định")
                                .issuedDate(LocalDate.of(2024, 6, 5))
                                .effectiveDate(LocalDate.of(2024, 6, 5))
                                .expiredDate(LocalDate.of(2025, 6, 5))
                                .issuer("Thủ tướng Chính phủ")
                                .summary(
                                                "Quyết định này quy định về mức hỗ trợ khẩn cấp đối với hộ gia đình bị thiệt hại do thiên tai, bao gồm hỗ trợ lương thực, nước uống, nhu yếu phẩm và hỗ trợ sửa chữa nhà ở.")
                                .build();
                p4 = policyRepository.save(p4);
                Chapter c4 = Chapter.builder()
                        .policy(p4)
                        .title("Chapter I")
                        .build();
                c4 = chapterRepository.save(c4);
                articleRepository.save(Article.builder()
                                .chapter(c4).articleNo(1)
                                .title("Đối tượng hỗ trợ")
                                .content("Hộ gia đình bị thiệt hại về người, tài sản do thiên tai gây ra, có xác nhận của UBND cấp xã.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p4)
                                .applicableSubject("Hộ gia đình")
                                .conditionValue("Bị thiệt hại do thiên tai (bão, lũ, hạn hán, sạt lở)")
                                .benchmark("Mức thiệt hại từ 30% tài sản trở lên")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p4)
                                .formula("Hỗ trợ khẩn cấp = 15kg gạo/người/tháng + Hỗ trợ nhà ở theo mức thiệt hại")
                                .benchmark("Tối đa 6 tháng hỗ trợ lương thực, tối đa 20 triệu VNĐ hỗ trợ sửa nhà")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 5: Health Insurance Support for the Poor =====
                Policy p5 = Policy.builder()
                                .documentNo("05/2024/ND-CP")
                                .title("Nghị định về hỗ trợ bảo hiểm y tế cho người nghèo")
                                .documentType("Nghị định")
                                .issuedDate(LocalDate.of(2024, 8, 1))
                                .effectiveDate(LocalDate.of(2024, 10, 1))
                                .expiredDate(null)
                                .issuer("Chính phủ")
                                .summary(
                                                "Nghị định quy định về việc ngân sách nhà nước hỗ trợ 100% mức đóng bảo hiểm y tế cho người thuộc hộ nghèo, và hỗ trợ 70% cho người thuộc hộ cận nghèo theo chuẩn nghèo đa chiều.")
                                .build();
                p5 = policyRepository.save(p5);
                Chapter c5 = Chapter.builder()
                        .policy(p5)
                        .title("Chapter I")
                        .build();
                c5 = chapterRepository.save(c5);
                articleRepository.save(Article.builder()
                                .chapter(c5).articleNo(1)
                                .title("Đối tượng")
                                .content(
                                                "Người thuộc hộ nghèo, hộ cận nghèo theo chuẩn nghèo đa chiều quốc gia. Người dân tộc thiểu số đang sinh sống tại vùng có điều kiện kinh tế - xã hội đặc biệt khó khăn.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p5)
                                .applicableSubject("Hộ nghèo")
                                .conditionValue("Thuộc danh sách hộ nghèo theo chuẩn quốc gia")
                                .benchmark("Được UBND cấp xã xác nhận và cập nhật hàng năm")
                                .build());
                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p5)
                                .applicableSubject("Hộ cận nghèo")
                                .conditionValue("Thuộc danh sách hộ cận nghèo theo chuẩn quốc gia")
                                .benchmark("Được hỗ trợ 70% mức đóng BHYT")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p5)
                                .formula("Mức đóng BHYT = Lương cơ sở × 4.5% × Mức hỗ trợ")
                                .benchmark("Lương cơ sở: 1.800.000 VNĐ. Hộ nghèo: 100%. Hộ cận nghèo: 70%.")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 6: Housing Support for War Veterans =====
                Policy p6 = Policy.builder()
                                .documentNo("06/2024/QD-TTg")
                                .title("Quyết định về hỗ trợ nhà ở cho người có công với cách mạng")
                                .documentType("Quyết định")
                                .issuedDate(LocalDate.of(2024, 9, 15))
                                .effectiveDate(LocalDate.of(2024, 11, 1))
                                .expiredDate(LocalDate.of(2026, 12, 31))
                                .issuer("Thủ tướng Chính phủ")
                                .summary(
                                                "Quyết định về chính sách hỗ trợ xây dựng, sửa chữa nhà ở đối với người có công với cách mạng và thân nhân liệt sĩ đang gặp khó khăn về nhà ở.")
                                .build();
                p6 = policyRepository.save(p6);
                Chapter c6 = Chapter.builder()
                        .policy(p6)
                        .title("Chapter I")
                        .build();
                c6 = chapterRepository.save(c6);
                articleRepository.save(Article.builder()
                                .chapter(c6).articleNo(1)
                                .title("Đối tượng")
                                .content(
                                                "Người hoạt động cách mạng trước năm 1945, người hoạt động cách mạng từ năm 1945 đến trước Tổng khởi nghĩa tháng 8/1945, thân nhân liệt sĩ đang hưởng trợ cấp hàng tháng.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p6)
                                .applicableSubject("Người có công")
                                .conditionValue("Có giấy chứng nhận người có công với cách mạng")
                                .benchmark("Hiện đang ở nhà tạm, nhà dột nát hoặc chưa có nhà ở")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p6)
                                .formula("Hỗ trợ xây mới: 40 triệu VNĐ/hộ. Hỗ trợ sửa chữa: 20 triệu VNĐ/hộ.")
                                .benchmark("Ngân sách trung ương hỗ trợ 70%, ngân sách địa phương 30%")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 7: Tuition Fee Exemption for Primary Students =====
                Policy p7 = Policy.builder()
                                .documentNo("07/2024/ND-CP")
                                .title("Nghị định về miễn giảm học phí cho học sinh phổ thông")
                                .documentType("Nghị định")
                                .issuedDate(LocalDate.of(2024, 10, 1))
                                .effectiveDate(LocalDate.of(2025, 1, 1))
                                .expiredDate(null)
                                .issuer("Chính phủ")
                                .summary(
                                                "Nghị định quy định về việc miễn học phí cho học sinh tiểu học công lập trên toàn quốc, giảm học phí cho học sinh trung học cơ sở thuộc hộ nghèo, hộ cận nghèo.")
                                .build();
                p7 = policyRepository.save(p7);
                Chapter c7 = Chapter.builder()
                        .policy(p7)
                        .title("Chapter I")
                        .build();
                c7 = chapterRepository.save(c7);
                articleRepository.save(Article.builder()
                                .chapter(c7).articleNo(1)
                                .title("Miễn học phí")
                                .content(
                                                "Học sinh tiểu học trường công lập được miễn 100% học phí. Học sinh trung học cơ sở thuộc hộ nghèo được giảm 70% học phí.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p7)
                                .applicableSubject("Học sinh tiểu học")
                                .conditionValue("Đang theo học tại trường tiểu học công lập")
                                .benchmark("Không yêu cầu thêm điều kiện")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p7)
                                .formula("Miễn 100% học phí cho tiểu học. Giảm 70% cho THCS hộ nghèo.")
                                .benchmark("Theo khung học phí do HĐND tỉnh quy định hàng năm")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 8: Funeral Support =====
                Policy p8 = Policy.builder()
                                .documentNo("08/2024/TT-BLDTBXH")
                                .title("Thông tư hướng dẫn hỗ trợ chi phí mai táng cho người nghèo")
                                .documentType("Thông tư")
                                .issuedDate(LocalDate.of(2024, 11, 10))
                                .effectiveDate(LocalDate.of(2025, 1, 1))
                                .expiredDate(null)
                                .issuer("Bộ Lao động - Thương binh và Xã hội")
                                .summary(
                                                "Thông tư hướng dẫn thực hiện hỗ trợ chi phí mai táng cho người thuộc hộ nghèo, người già cô đơn không nơi nương tựa khi qua đời.")
                                .build();
                p8 = policyRepository.save(p8);
                Chapter c8 = Chapter.builder()
                        .policy(p8)
                        .title("Chapter I")
                        .build();
                c8 = chapterRepository.save(c8);
                articleRepository.save(Article.builder()
                                .chapter(c8).articleNo(1)
                                .title("Mức hỗ trợ")
                                .content(
                                                "Mức hỗ trợ mai táng phí bằng 20 lần mức chuẩn trợ giúp xã hội áp dụng tại thời điểm đề nghị hỗ trợ.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p8)
                                .applicableSubject("Đối tượng bảo trợ xã hội")
                                .conditionValue("Thuộc diện hộ nghèo, người già cô đơn, người khuyết tật nặng")
                                .benchmark("Có xác nhận của UBND cấp xã")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p8)
                                .formula("Hỗ trợ mai táng = Chuẩn trợ giúp xã hội × 20")
                                .benchmark("Chuẩn trợ giúp xã hội: 500.000 VNĐ. Mức hỗ trợ: 10.000.000 VNĐ.")
                                .multiplier(20.0)
                                .build());

                // ===== Policy 9: Maternity Support for Female Workers =====
                Policy p9 = Policy.builder()
                                .documentNo("09/2024/QD-TTg")
                                .title("Quyết định về chế độ thai sản cho lao động nữ khu vực phi chính thức")
                                .documentType("Quyết định")
                                .issuedDate(LocalDate.of(2024, 12, 1))
                                .effectiveDate(LocalDate.of(2025, 3, 1))
                                .expiredDate(null)
                                .issuer("Thủ tướng Chính phủ")
                                .summary(
                                                "Quyết định về chính sách hỗ trợ thai sản cho lao động nữ làm việc trong khu vực phi chính thức, không tham gia bảo hiểm xã hội bắt buộc, bao gồm trợ cấp một lần khi sinh con.")
                                .build();
                p9 = policyRepository.save(p9);
                Chapter c9 = Chapter.builder()
                        .policy(p9)
                        .title("Chapter I")
                        .build();
                c9 = chapterRepository.save(c9);
                articleRepository.save(Article.builder()
                                .chapter(c9).articleNo(1)
                                .title("Điều kiện hưởng")
                                .content(
                                                "Lao động nữ thuộc hộ nghèo hoặc cận nghèo, không tham gia BHXH bắt buộc, sinh con trong thời gian áp dụng chính sách.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p9)
                                .applicableSubject("Lao động nữ")
                                .conditionValue("Thuộc hộ nghèo/cận nghèo, không tham gia BHXH bắt buộc")
                                .benchmark("Sinh con trong thời gian chính sách có hiệu lực")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p9)
                                .formula("Trợ cấp một lần = 2.000.000 VNĐ/1 con")
                                .benchmark("Không phụ thuộc vào số lần sinh")
                                .multiplier(1.0)
                                .build());

                // ===== Policy 10: Clean Water Support for Rural Areas =====
                Policy p10 = Policy.builder()
                                .documentNo("10/2024/QD-TTg")
                                .title("Quyết định về hỗ trợ nước sạch cho hộ gia đình vùng nông thôn")
                                .documentType("Quyết định")
                                .issuedDate(LocalDate.of(2024, 12, 15))
                                .effectiveDate(LocalDate.of(2025, 2, 1))
                                .expiredDate(LocalDate.of(2027, 12, 31))
                                .issuer("Thủ tướng Chính phủ")
                                .summary(
                                                "Quyết định về chính sách hỗ trợ lắp đặt hệ thống nước sạch, xây dựng công trình vệ sinh cho hộ gia đình nghèo, cận nghèo tại các xã nông thôn vùng khó khăn.")
                                .build();
                p10 = policyRepository.save(p10);
                Chapter c10 = Chapter.builder()
                        .policy(p10)
                        .title("Chapter I")
                        .build();
                c10 = chapterRepository.save(c10);
                articleRepository.save(Article.builder()
                                .chapter(c10).articleNo(1)
                                .title("Hỗ trợ")
                                .content(
                                                "Mỗi hộ gia đình được hỗ trợ tối đa 5 triệu VNĐ để lắp đặt hệ thống nước sạch và công trình vệ sinh đạt chuẩn.")
                                .build());

                eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                                .policy(p10)
                                .applicableSubject("Hộ gia đình")
                                .conditionValue("Thuộc hộ nghèo hoặc hộ cận nghèo tại các xã nông thôn")
                                .benchmark("Cư trú tại xã thuộc vùng khó khăn theo quy định")
                                .build());

                benefitRuleRepository.save(BenefitRule.builder()
                                .policy(p10)
                                .formula("Hỗ trợ tối đa 5.000.000 VNĐ/hộ. Ngân sách TW 70%, địa phương 30%.")
                                .benchmark("Hỗ trợ một lần, không quá 5 năm mới được hỗ trợ lại")
                                .multiplier(1.0)
                                .build());

                System.out.println(">>> Seeded " + policyRepository.count()
                                + " sample policies with articles, eligibility criteria, and benefit rules.");
        }
}