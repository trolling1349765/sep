package fpt.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Date;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
@SuperBuilder
@NoArgsConstructor
public class BaseEntity {

    @Column(name = "create_at")
    LocalDate createAt;

    @Column(name = "create_by")
    String createBy;

    @Column(name = "update_at")
    LocalDate updateAt;

    @Column(name = "update_by")
    String updateBy;

    @Column(name = "is_delete")
    boolean isDelete = false;

}
