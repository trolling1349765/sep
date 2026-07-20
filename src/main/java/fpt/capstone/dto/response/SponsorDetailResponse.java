package fpt.capstone.dto.response;

import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Sponsor detail (GET /sponsors/{id}) plus the contribution-history tab.
 * {@code getId()} is what {@code AuditAspect} reflects for the audit entityId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorDetailResponse {

    private String id;
    private String code;
    private String name;
    private SponsorType type;
    private String orgCode;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private String note;
    private SponsorStatus status;
    private LocalDate createdAt;
    private String createdBy;
    private List<ContributionHistoryItem> contributionHistory;

    public static SponsorDetailResponse from(Sponsor sponsor, List<ContributionHistoryItem> history) {
        return SponsorDetailResponse.builder()
                .id(sponsor.getId())
                .code(sponsor.getCode())
                .name(sponsor.getName())
                .type(sponsor.getType())
                .orgCode(sponsor.getOrgCode())
                .contactPerson(sponsor.getContactPerson())
                .phone(sponsor.getPhone())
                .email(sponsor.getEmail())
                .address(sponsor.getAddress())
                .note(sponsor.getNote())
                .status(sponsor.getStatus())
                .createdAt(sponsor.getCreateAt())
                .createdBy(sponsor.getCreateBy())
                .contributionHistory(history)
                .build();
    }
}
