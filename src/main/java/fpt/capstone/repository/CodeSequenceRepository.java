package fpt.capstone.repository;

import fpt.capstone.entity.CodeSequence;
import fpt.capstone.entity.CodeSequenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeSequenceId> {

    /**
     * Atomically insert-or-increment the (prefix, year) counter. InnoDB serializes concurrent
     * statements on the same key via the row/gap lock, so two callers never collide.
     */
    @Modifying
    @Query(value = "insert into code_sequences(prefix, year_value, seq_value) values (:prefix, :year, 1) "
            + "on duplicate key update seq_value = seq_value + 1", nativeQuery = true)
    void upsertIncrement(@Param("prefix") String prefix, @Param("year") int year);

    @Query(value = "select seq_value from code_sequences where prefix = :prefix and year_value = :year",
            nativeQuery = true)
    long currentValue(@Param("prefix") String prefix, @Param("year") int year);
}
