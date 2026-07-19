package fpt.capstone.repository;

import fpt.capstone.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    @Query("select p.right.code from Permission p where p.role.id = :roleId")
    Set<String> findRightCodesByRoleId(@Param("roleId") int roleId);

    @Query("select p.right.id from Permission p where p.role.id = :roleId")
    Set<Integer> findRightIdsByRoleId(@Param("roleId") int roleId);

    long countByRoleId(int roleId);

    @Modifying
    @Query("delete from Permission p where p.role.id = :roleId and p.right.id in :rightIds")
    void deleteByRoleIdAndRightIdIn(@Param("roleId") int roleId, @Param("rightIds") Collection<Integer> rightIds);

    interface RoleGrantCount {
        int getRoleId();

        long getGrantedCount();
    }

    @Query("select p.role.id as roleId, count(p) as grantedCount from Permission p group by p.role.id")
    List<RoleGrantCount> countGrantsGroupedByRole();
}
