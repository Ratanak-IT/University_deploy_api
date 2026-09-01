package com.universitymanagement.grading.security;

import com.universitymanagement.assignment.exception.AssignmentClassroomNotFoundException;
import com.universitymanagement.assignment.exception.ClassroomHasNoTeacherException;
import com.universitymanagement.assignment.exception.NotClassroomTeacherException;
import com.universitymanagement.assignment.exception.TeacherProfileNotFoundException;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;
import com.universitymanagement.classroom.repository.ClassroomMemberRepository;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.teacher.entity.Teacher;
import com.universitymanagement.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Who may touch a classroom's grades. Teachers get their own classrooms;
 * admins get all of them; nobody else gets any.
 */
@Component
@RequiredArgsConstructor
public class ClassroomGradeGuard {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public Classroom requireClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AssignmentClassroomNotFoundException(classroomId));
    }

    /** Write access. Returns the teacher the change should be attributed to. */
    public Teacher requireGrader(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (hasRole(auth, "ADMIN")) {
            if (classroom.getTeacher() == null) {
                throw new ClassroomHasNoTeacherException(classroom.getClassroomId());
            }
            return classroom.getTeacher();
        }

        User user = currentUser(auth);
        Teacher teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(TeacherProfileNotFoundException::new);

        // Two things make someone a teacher of a classroom, and both count.
        //
        // `classroom.teacher` is the lead — one person, used for attribution and
        // as the default when an admin acts. Membership in classroom_members is
        // the wider teaching staff, and it is what the People tab lists.
        // Checking only the lead meant a second teacher could be added to a
        // class, see themselves listed on it, and still be refused by every
        // screen that writes: the invitation looked real and granted nothing.
        boolean isLead = classroom.getTeacher() != null
                && classroom.getTeacher().getTeacherId().equals(teacher.getTeacherId());

        boolean isMember = memberRepository
                .existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
                        classroom.getClassroomId(), user.getId(),
                        ClassroomRole.TEACHER, MemberStatus.ACTIVE);

        if (!isLead && !isMember) {
            throw new NotClassroomTeacherException(classroom.getClassroomId());
        }
        return teacher;
    }

    /** Read access — admins need no teacher assigned on the classroom. */
    public void requireReader(Classroom classroom) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasRole(auth, "ADMIN")) {
            return;
        }
        requireGrader(classroom);
    }

    public boolean isAdmin() {
        return hasRole(SecurityContextHolder.getContext().getAuthentication(), "ADMIN");
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }
}
