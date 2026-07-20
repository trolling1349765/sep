package fpt.capstone.integration;

import fpt.capstone.repository.CodeSequenceRepository;
import fpt.capstone.util.CodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves CodeGenerator.next() is safe under contention: 20 threads racing on one (prefix, year)
 * counter produce 20 distinct, contiguous codes with no gaps or duplicates.
 * Runs against the real Testcontainers MySQL so the InnoDB row serialization is actually exercised.
 */
class CodeGeneratorConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private CodeSequenceRepository codeSequenceRepository;

    @Test
    void next_under20ConcurrentThreads_producesUniqueContiguousCodes() throws Exception {
        int threads = 20;
        String prefix = "T" + uniq();           // unique per run -> a fresh (prefix, year) row
        int year = Year.now().getValue();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<String>> tasks = IntStream.range(0, threads)
                .<Callable<String>>mapToObj(i -> () -> {
                    start.await();
                    return codeGenerator.next(prefix);
                })
                .toList();

        try {
            List<Future<String>> futures = tasks.stream().map(pool::submit).toList();
            start.countDown();

            List<String> codes = new ArrayList<>();
            for (Future<String> f : futures) {
                codes.add(f.get());
            }

            // Unique
            Set<String> distinct = Set.copyOf(codes);
            assertEquals(threads, distinct.size(), "codes must be unique: " + codes);

            // Correct format PREFIX-yyyy-nnn
            String expectedHead = prefix + "-" + year + "-";
            assertTrue(codes.stream().allMatch(c -> c.startsWith(expectedHead)),
                    "all codes share PREFIX-year-: " + codes);

            // Sequence numbers are exactly 1..20 (contiguous, no gaps)
            Set<Integer> seqNums = codes.stream()
                    .map(c -> Integer.parseInt(c.substring(c.lastIndexOf('-') + 1)))
                    .collect(Collectors.toSet());
            Set<Integer> expected = IntStream.rangeClosed(1, threads).boxed().collect(Collectors.toSet());
            assertEquals(expected, seqNums, "sequence numbers must be 1..20 contiguous: " + codes);

            // Committed counter reflects all 20 increments
            assertEquals(20L, codeSequenceRepository.currentValue(prefix, year));
        } finally {
            pool.shutdown();
        }
    }
}
