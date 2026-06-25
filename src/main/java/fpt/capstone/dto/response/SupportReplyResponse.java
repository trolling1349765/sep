package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportReplyResponse {
    private String id;
    private String officerId;
    private String officerName;
    private String message;
    private Instant createdAt;
}