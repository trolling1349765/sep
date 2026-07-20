package fpt.capstone.util;

import fpt.capstone.repository.CodeSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Generates human-readable document codes {@code PREFIX-yyyy-nnn} (nnn zero-padded to 3 digits),
 * e.g. {@code NTT-2026-001}. Prefixes used by the donor/resource module:
 * NTT (sponsor), KP (funding), KH (fund plan), VP (support item), PNK (inbound receipt),
 * KHPP (item allocation plan), CP (distribution).
 *
 * <p>Runs in its own transaction ({@link Propagation#REQUIRES_NEW}) so the counter advances
 * independently of the business transaction — a business rollback never rolls the counter back
 * (numbering gaps are acceptable). The atomic UPSERT serializes concurrent callers on the
 * (prefix, year) row.
 */
@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private final CodeSequenceRepository codeSequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String prefix) {
        int year = Year.now().getValue();
        codeSequenceRepository.upsertIncrement(prefix, year);
        long value = codeSequenceRepository.currentValue(prefix, year);
        return String.format("%s-%d-%03d", prefix, year, value);
    }
}
