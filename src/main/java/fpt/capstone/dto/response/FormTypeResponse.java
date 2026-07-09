package fpt.capstone.dto.response;

import fpt.capstone.entity.FormType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormTypeResponse {
    String name;
//    Integer id;
//    Integer policyId;

    public FormTypeResponse(FormType formType) {
        this.name = formType.getName();
//        this.id = formType.getId();
//        this.policyId = formType.getPolicy().getId();
    }
}
