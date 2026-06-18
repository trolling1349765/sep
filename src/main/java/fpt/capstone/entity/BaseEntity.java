package fpt.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public class BaseEntity {

    @Column(name = "create_at")
    Date createAt;

    @Column(name = "create_by")
    String createBy;

    @Column(name = "update_at")
    Date updateAt;

    @Column(name = "update_by")
    String updateBy;

    @Column(name = "is_delete")
    boolean isDelete = false;

}
