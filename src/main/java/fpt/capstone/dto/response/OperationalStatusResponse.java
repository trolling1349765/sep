package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalStatusResponse {
    private String status;
    private LocalDateTime serverTime;
    private Instant startedAt;
    private String version;
    private String environment;
    private List<String> simulatedServices;
}
