package fpt.capstone.entity;

import java.util.Date;

public class BaseEntity {

    default Date createDate;
    default Date updateDate;
    default boolean isDelete;
}
