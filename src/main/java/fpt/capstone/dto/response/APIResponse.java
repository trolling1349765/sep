package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
public class APIResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> APIResponse<T> success(T data) {
        return APIResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> APIResponse<T> success(String message, T data) {
        return APIResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> APIResponse<T> error(int code, String message) {
        return APIResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
