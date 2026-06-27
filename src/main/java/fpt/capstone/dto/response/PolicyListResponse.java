package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyListResponse {
    private int id;
    private String title;
    private String summary;
    private String documentType;
    private String documentNo;
    private LocalDate issuedDate;
}