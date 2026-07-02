package fpt.capstone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "benificiaries")
public class Benificiary extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<DistributionRecord> distributionRecords;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<BenefitHistory> benefitHistories;

    @JsonIgnore
    @OneToMany(mappedBy = "benificiary")
    List<GoodsDistribution> goodsDistributionList;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "application_id")
    Application application;

    @Column(name = "gender")
    boolean gender;

    @Column(name = "assistance_amount")
    Double assistanceAmount;

    @Column(name = "ho_ten")
    String hoTen;

    @Column(name = "bi_danh")
    String biDanh;

    @Column(name = "ngay_sinh")
    LocalDate ngaySinh;

    @Column(name = "cccd")
    String CCCD;

    @Column(name = "ngay_cap")
    LocalDate ngayCap;

    @Column(name = "noi_cap")
    String noiCap;

    @Column(name = "que_quan")
    String quaQuan;

    @Column(name = "noi_thuong_tru")
    String noiThuongTru;

    @Column(name = "ngay_vao_dang")
    LocalDate ngayVaoDang;

    @Column(name = "ngay_chinh_thuc")
    LocalDate ngayChinhThuc;

    @Column(name = "cap_bac")
    String capBac;

    @Column(name = "don_vi")
    String donVi;

    @Column(name = "bat_dau_hoat_dong_cach_mang")
    LocalDate batDauHoatDongCachMang;

    @Column(name = "ket_thuc_hoat_dong_cach_mang")
    LocalDate ketThucHoatDongCachMang;

    @OneToMany(mappedBy = "benificiary")
    List<WoundedSoldiers>  woundedSoldiers;

    @Column(name = "ngay_hy_sinh")
    LocalDate ngayHySinh;

    @Column(name = "don_vi_khi_hy_sinh")
    String donViKhiHySinh;

    @Column(name = "cap_bac_khi_hy_sinh")
    String capBacKhiHySinh;

    @Column(name = "bang_to_quoc_ghi_cong_so")
    Integer bangToQuocGhiCongSo;

    @Column(name = "so_quyet_dinh_khi_ghi_cong")
    Integer soQuyetDinhKhiGhiCong;

    @Column(name = "ngay_ghi_cong")
    LocalDate ngayGhiCong;

}
