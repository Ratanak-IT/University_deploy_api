package com.universitymanagement.student.service;

import com.universitymanagement.student.dto.request.TransferCreditRequest;
import com.universitymanagement.student.dto.response.TransferCreditResponse;

import java.util.List;
import java.util.UUID;

public interface TransferCreditService {
    TransferCreditResponse create(TransferCreditRequest request);

    List<TransferCreditResponse> getForStudent(UUID studentId);

    void delete(UUID transferCreditId);
}
