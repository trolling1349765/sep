package fpt.capstone.dto.response;

import fpt.capstone.enums.SupportCategory;
import fpt.capstone.enums.SupportRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportRequestListResponse {
    private String id;
    private String referenceNumber;
    private String citizenName;
    private SupportCategory category;
    private String subject;
    private SupportRequestStatus status;
    private LocalDate submissionDate;
}