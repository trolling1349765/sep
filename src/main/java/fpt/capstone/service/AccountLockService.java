package fpt.capstone.service;

import fpt.capstone.entity.User;
import fpt.capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockService {

    private final UserRepository userRepository;

    @Value("${auth.lock.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${auth.lock.duration-minutes}")
    private int lockDurationMinutes;

    // REQUIRES_NEW: the caller (login) throws 401 right after recording the
    // attempt, which rolls its transaction back - the counter and the lock
    // must survive that rollback or lockout never takes effect.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            Instant lockUntil = Instant.now().plusSeconds(lockDurationMinutes * 60L);
            user.setLockedUntil(lockUntil);
            log.warn("Account locked for user {} until {}", user.getEmail(), lockUntil);
        }

        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }

        if (Instant.now().isAfter(user.getLockedUntil())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            log.info("Account auto-unlocked for user {}", user.getEmail());
            return false;
        }

        return true;
    }

    @Transactional
    public void unlockAccount(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Account unlocked for user {}", user.getEmail());
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public long getLockRemainingSeconds(User user) {
        if (user.getLockedUntil() == null) {
            return 0;
        }
        return Math.max(0, user.getLockedUntil().getEpochSecond() - Instant.now().getEpochSecond());
    }
}