package com.universitymanagement.academicterm.repository;

import com.universitymanagement.academicterm.entity.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {
}
