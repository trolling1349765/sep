package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One event in a plan's lifecycle timeline (shared by fund plans and item plans). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTimelineEntry {

    private String status;
    private String by;
    private LocalDateTime at;
    private String reason;
}
