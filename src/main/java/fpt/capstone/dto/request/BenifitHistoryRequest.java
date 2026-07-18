package fpt.capstone.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
public class BenifitHistoryRequest {
    Integer id;
    Integer benificiaryId;
    String deliverId;
    String transferMethod;
    String receiver;
}
