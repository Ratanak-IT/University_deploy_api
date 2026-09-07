package com.universitymanagement.student.service.impl;

import com.universitymanagement.student.dto.request.TransferCreditRequest;
import com.universitymanagement.student.dto.response.TransferCreditResponse;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.entity.TransferCredit;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.student.repository.TransferCreditRepository;
import com.universitymanagement.student.service.TransferCreditService;
import com.universitymanagement.subject.entity.Subject;
import com.universitymanagement.subject.exception.SubjectNotFoundException;
import com.universitymanagement.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferCreditServiceImpl implements TransferCreditService {

    private final TransferCreditRepository transferCreditRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public TransferCreditResponse create(TransferCreditRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Subject subject = null;
        if (request.subjectId() != null) {
            subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));
        }

        TransferCredit credit = new TransferCredit();
        credit.setStudent(student);
        credit.setSubject(subject);
        credit.setSourceInstitution(request.sourceInstitution());
        credit.setCredits(request.credits());
        credit.setGrantedDate(request.grantedDate());

        return toResponse(transferCreditRepository.save(credit));
    }

    @Override
    public List<TransferCreditResponse> getForStudent(UUID studentId) {
        return transferCreditRepository.findByStudent_StudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID transferCreditId) {
        transferCreditRepository.deleteById(transferCreditId);
    }

    private TransferCreditResponse toResponse(TransferCredit credit) {
        return new TransferCreditResponse(
                credit.getTransferCreditId(),
                credit.getStudent().getStudentId(),
                credit.getSubject() != null ? credit.getSubject().getSubjectId() : null,
                credit.getSubject() != null ? credit.getSubject().getSubjectName() : null,
                credit.getSourceInstitution(),
                credit.getCredits(),
                credit.getGrantedDate()
        );
    }
}
