package com.universitymanagement.identity.repository;

import com.universitymanagement.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Ids of every user holding a role, matched case-insensitively and with any
     * {@code ROLE_} prefix ignored — the same name is written both ways in
     * different parts of this system.
     *
     * <p>Returns ids rather than users because the only caller addresses
     * notifications, and loading whole rows to read one column of each would be
     * wasted work on every event.
     */
    @Query("""
            select ur.user.id from UserRole ur
            where upper(ur.role.name) in (:names)
            """)
    List<UUID> findUserIdsByRoleNames(@Param("names") List<String> names);
}
