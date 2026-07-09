package fpt.capstone.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationRequest {
    Integer id;
    String approvedBy;
    LocalDate approvedDate;
    LocalDate submitDate;
    String status;
    Integer formTypeId;
    boolean isDeleted;
}
