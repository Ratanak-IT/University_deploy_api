package com.universitymanagement.attendance.service.impl;

import com.universitymanagement.assignment.exception.AssignmentClassroomNotFoundException;
import com.universitymanagement.assignment.exception.NotClassroomTeacherException;
import com.universitymanagement.assignment.exception.TeacherProfileNotFoundException;
import com.universitymanagement.attendance.dto.request.AttendanceItemRequest;
import com.universitymanagement.attendance.dto.request.RecordAttendanceRequest;
import com.universitymanagement.attendance.dto.response.AttendanceResponse;
import com.universitymanagement.attendance.entity.Attendance;
import com.universitymanagement.attendance.repository.AttendanceRepository;
import com.universitymanagement.attendance.service.AttendanceService;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.score.exception.StudentNotInClassroomException;
import com.universitymanagement.student.entity.Student;
import com.universitymanagement.student.repository.StudentRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<AttendanceResponse> recordClassroomAttendance(UUID classroomId, RecordAttendanceRequest request) {
        Classroom classroom = findClassroom(classroomId);
        requireTeacherOwnsClassroomOrAdmin(classroom);

        List<Attendance> savedList = request.items().stream().map(item -> {
            Student student = studentRepository.findById(item.studentId())
                    .orElseThrow(() -> new StudentNotInClassroomException(item.studentId(), classroomId));

            boolean enrolled = classroomStudentRepository
                    .existsByClassroom_ClassroomIdAndStudent_StudentId(classroomId, student.getStudentId());
            if (!enrolled) {
                throw new StudentNotInClassroomException(student.getStudentId(), classroomId);
            }

            Attendance attendance = attendanceRepository
                    .findByStudent_StudentIdAndClassroom_ClassroomIdAndAttendanceDate(
                            student.getStudentId(), classroomId, request.attendanceDate())
                    .orElseGet(Attendance::new);

            attendance.setStudent(student);
            attendance.setClassroom(classroom);
            attendance.setAttendanceDate(request.attendanceDate());
            attendance.setStatus(item.status());
            attendance.setRemark(item.remark());

            return attendanceRepository.save(attendance);
        }).toList();

        return savedList.stream().map(this::toResponse).toList();
    }

    @Override
    public List<AttendanceResponse> getClassroomAttendanceByDate(UUID classroomId, LocalDate date) {
        Classroom classroom = findClassroom(classroomId);
        requireTeacherOwnsClassroomOrAdmin(classroom);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<Attendance> records = attendanceRepository
                .findByClassroom_ClassroomIdAndAttendanceDate(classroomId, targetDate);

        return records.stream().map(this::toResponse).toList();
    }

    private AttendanceResponse toResponse(Attendance a) {
        return new AttendanceResponse(
                a.getAttendanceId(),
                a.getClassroom().getClassroomId(),
                a.getClassroom().getClassName(),
                a.getClassroom().getSubject() != null ? a.getClassroom().getSubject().getSubjectName() : null,
                a.getStudent() != null ? a.getStudent().getStudentId() : null,
                a.getStudent() != null ? a.getStudent().getStudentCode() : null,
                a.getStudent() != null && a.getStudent().getUser() != null ? a.getStudent().getUser().getFullName() : null,
                a.getAttendanceDate(),
                a.getStatus(),
                a.getRemark()
        );
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AssignmentClassroomNotFoundException(classroomId));
    }

    private void requireTeacherOwnsClassroomOrAdmin(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(auth, "ADMIN")) {
            return;
        }

        User user = getCurrentUser(auth);
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(TeacherProfileNotFoundException::new);

        boolean owns = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());
        if (!owns) {
            throw new NotClassroomTeacherException(classroom.getClassroomId());
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }
}
