package com.universitymanagement.program.repository;

import com.universitymanagement.program.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {
    boolean existsByProgramNameIgnoreCase(String programName);
}
