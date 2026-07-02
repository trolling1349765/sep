package fpt.capstone.dto.request;

import fpt.capstone.entity.AdditionalDocument;
import fpt.capstone.entity.DecisionDocument;
import fpt.capstone.entity.Policy;
import fpt.capstone.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationRequest {
    Integer id;
    String supportedUser;
    String approvedBy;
    LocalDate approvedDate;
    int policyId;
    LocalDate submitDate;
    String status;
    String formType;
    boolean isDelete;
}
