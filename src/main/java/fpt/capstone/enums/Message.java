package fpt.capstone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum Message {
    MSG_NOT_FOUND("không tìm thấy kết quả"),
    ;

    private String message;
}
