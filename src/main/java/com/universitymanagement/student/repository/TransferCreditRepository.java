package com.universitymanagement.student.repository;

import com.universitymanagement.student.entity.TransferCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferCreditRepository extends JpaRepository<TransferCredit, UUID> {
    List<TransferCredit> findByStudent_StudentId(UUID studentId);
}
