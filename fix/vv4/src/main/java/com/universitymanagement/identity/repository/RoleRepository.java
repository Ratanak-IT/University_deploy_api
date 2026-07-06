package com.universitymanagement.identity.repository;

import com.universitymanagement.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
