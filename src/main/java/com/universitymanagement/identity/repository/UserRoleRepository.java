package com.universitymanagement.identity.repository;

import com.universitymanagement.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}
