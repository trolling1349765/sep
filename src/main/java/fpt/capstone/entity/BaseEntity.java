package fpt.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.util.Date;

@MappedSuperclass
@Data
public class BaseEntity {

    protected Date createAt;
    protected String createBy;
    protected Date updateAt;
    protected String updateBy;
    protected boolean isDelete = false;

}
