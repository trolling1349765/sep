package fpt.capstone.service;

import java.util.Set;

public interface PermissionCacheService {

    /**
     * Right codes granted to the given role, served from an in-memory cache
     * (TTL-bounded). Loaded from DB on miss.
     */
    Set<String> getRightCodes(int roleId);

    /**
     * Drops the whole cache. Called after any successful permission/right write so
     * grants take effect on the next request without re-login.
     */
    void evictAll();
}
