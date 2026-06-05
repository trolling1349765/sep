package fpt.capstone.entity;

import jakarta.persistence.Column;

import java.util.Date;


public class BaseEntity {

    @Column(name = "Create_At")
    protected Date createAt;

    @Column(name = "Create_By")
    protected String createBy;

    @Column(name = "Update_At")
    protected Date updateAt;

    @Column(name = "Update_By")
    protected String updateBy;

    @Column(name = "Is_Delete")
    protected boolean isDelete;

    @Column(name = "Delete_By")
    protected String deleteBy;
}
