package fpt.capstone.dto.response;

import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.SponsorStatus;
import fpt.capstone.enums.SponsorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Row of the sponsor list (GET /sponsors). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorListResponse {

    private String id;
    private String code;
    private String name;
    private SponsorType type;
    private String contactPerson;
    private String phone;
    private SponsorStatus status;
    private LocalDate createdAt;

    public static SponsorListResponse from(Sponsor sponsor) {
        return SponsorListResponse.builder()
                .id(sponsor.getId())
                .code(sponsor.getCode())
                .name(sponsor.getName())
                .type(sponsor.getType())
                .contactPerson(sponsor.getContactPerson())
                .phone(sponsor.getPhone())
                .status(sponsor.getStatus())
                .createdAt(sponsor.getCreateAt())
                .build();
    }
}
