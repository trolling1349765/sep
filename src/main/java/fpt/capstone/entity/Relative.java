package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "relatives")
public class Relative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "ho_ten")
    String hoTen;

    LocalDate ngaySinh;
    String CCCD;
    LocalDate ngayCap;
    String noiCap;
    String quanHeVoiNguoiHoatDongCachMang;

    @Column(name = "noi_thuong_tru")
    String noiThuongTru;

    @Column(name = "phone")
    String phone;

    @Column(name = "email")
    String email;

    @Column(name = "gender")
    boolean gender;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "application_id")
    Application application;
}
