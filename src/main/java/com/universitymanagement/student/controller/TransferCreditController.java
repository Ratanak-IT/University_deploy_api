package com.universitymanagement.student.controller;

import com.universitymanagement.student.dto.request.TransferCreditRequest;
import com.universitymanagement.student.dto.response.TransferCreditResponse;
import com.universitymanagement.student.service.TransferCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/transfer-credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TransferCreditController {

    private final TransferCreditService transferCreditService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public TransferCreditResponse create(@Valid @RequestBody TransferCreditRequest request) {
        return transferCreditService.create(request);
    }

    @GetMapping("/student/{studentId}")
    public List<TransferCreditResponse> getForStudent(@PathVariable UUID studentId) {
        return transferCreditService.getForStudent(studentId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{transferCreditId}")
    public void delete(@PathVariable UUID transferCreditId) {
        transferCreditService.delete(transferCreditId);
    }
}
