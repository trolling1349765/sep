package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import fpt.capstone.entity.Right;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RightResponse {

    private int id;
    private String code;
    private String name;
    private String description;
    private String module;
    private String moduleName;

    @JsonProperty("isSystem")
    private boolean isSystem;
    private Integer sortOrder;

    public static RightResponse from(Right right) {
        return RightResponse.builder()
                .id(right.getId())
                .code(right.getCode())
                .name(right.getName())
                .description(right.getDescription())
                .module(right.getModule())
                .moduleName(right.getModuleName())
                .isSystem(right.isSystem())
                .sortOrder(right.getSortOrder())
                .build();
    }
}
