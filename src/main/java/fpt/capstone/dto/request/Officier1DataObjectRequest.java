package fpt.capstone.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Officier1DataObjectRequest {

    ApplicationRequest applicationRequest;
    BenificiaryRequest benificiaryRequest;
    RelativeRequest relativeRequest;
    WounderSoldierRequest wounderSoldierRequest;
}
