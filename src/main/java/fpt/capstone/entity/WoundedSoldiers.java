package fpt.capstone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(name = "wounded_soldiers")
public class WoundedSoldiers extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benificiary")
    Benificiary benificiary;

    Integer lanThu;
    LocalDate ngayNhapNgu;
    LocalDate ngayXuatngu;
    String donViKhiBiThuong;
    LocalDate ngayBiThuong;
    String capBacKhiBiThuong;
    String noiBiThuong;
    String vetThuong;
    String noiDieuTriSauKhiBiThuong;
    LocalDate ngayRaVien;
}
