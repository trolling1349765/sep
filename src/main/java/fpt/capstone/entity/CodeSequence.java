package fpt.capstone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Monotonic per-(prefix, year) counter backing {@code CodeGenerator} — produces PREFIX-yyyy-nnn codes.
 * Infrastructure only; deliberately does NOT extend BaseEntity.
 */
@Entity
@Table(name = "code_sequences")
@IdClass(CodeSequenceId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeSequence {

    @Id
    @Column(name = "prefix", length = 16, nullable = false)
    private String prefix;

    // "year" is a reserved word in MySQL — map to a safe column name.
    @Id
    @Column(name = "year_value", nullable = false)
    private int year;

    // "last_value" collides with MySQL 8's LAST_VALUE reserved word — map to a safe column name.
    @Column(name = "seq_value", nullable = false)
    private long lastValue;
}
