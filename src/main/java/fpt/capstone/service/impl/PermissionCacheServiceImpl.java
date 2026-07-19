package fpt.capstone.service.impl;

import fpt.capstone.repository.PermissionRepository;
import fpt.capstone.service.PermissionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PermissionCacheServiceImpl implements PermissionCacheService {

    private static final long TTL_MS = 10 * 60 * 1000L;

    private record Entry(Set<String> codes, long loadedAt) {
    }

    private final ConcurrentHashMap<Integer, Entry> cache = new ConcurrentHashMap<>();
    private final PermissionRepository permissionRepository;

    @Override
    public Set<String> getRightCodes(int roleId) {
        Entry entry = cache.get(roleId);
        if (entry != null && now() - entry.loadedAt() < TTL_MS) {
            return entry.codes();
        }
        Set<String> codes = Set.copyOf(permissionRepository.findRightCodesByRoleId(roleId));
        cache.put(roleId, new Entry(codes, now()));
        return codes;
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    // Seam for TTL tests.
    protected long now() {
        return System.currentTimeMillis();
    }
}
