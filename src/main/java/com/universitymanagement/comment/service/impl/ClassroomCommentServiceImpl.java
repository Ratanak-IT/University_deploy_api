package com.universitymanagement.comment.service.impl;

import com.universitymanagement.assignment.entity.Assignment;
import com.universitymanagement.assignment.repository.AssignmentRepository;
import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;
import com.universitymanagement.classroom.entity.Classroom;
import com.universitymanagement.classroom.entity.ClassroomMember;
import com.universitymanagement.classroom.entity.ClassroomStudent;
import com.universitymanagement.classroom.repository.ClassroomMemberRepository;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.repository.ClassroomStudentRepository;
import com.universitymanagement.comment.dto.request.CommentCreateRequest;
import com.universitymanagement.comment.dto.request.CommentUpdateRequest;
import com.universitymanagement.comment.dto.response.CommentResponse;
import com.universitymanagement.comment.dto.response.MentionUserResponse;
import com.universitymanagement.comment.entity.ClassroomComment;
import com.universitymanagement.comment.entity.CommentMention;
import com.universitymanagement.comment.repository.ClassroomCommentRepository;
import com.universitymanagement.identity.entity.User;
import com.universitymanagement.identity.exception.UserNotFoundException;
import com.universitymanagement.identity.repository.UserRepository;
import com.universitymanagement.minio.MinioService;
import com.universitymanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.universitymanagement.comment.service.ClassroomCommentService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomCommentServiceImpl implements ClassroomCommentService {


    private static final String TEACHER_CLASSROOM_ROUTE = "/dashboard/teacher/my-classroom/%s?comment=%s";
    private static final String STUDENT_CLASSROOM_ROUTE = "/dashboard/student/my-classes/%s?comment=%s";


    private static final String TEACHER_ASSIGNMENT_ROUTE =
            "/dashboard/teacher/assignments/%s?comment=%s";
    private static final String STUDENT_ASSIGNMENT_ROUTE =
            "/dashboard/student/courses/assignment?classroomId=%s&assignmentId=%s&comment=%s";

    private static final String TYPE_MENTION = "MENTION";
    private static final String TYPE_REPLY = "COMMENT_REPLY";
    private static final String RESOURCE_CLASSROOM = "CLASSROOM";
    private static final String RESOURCE_ASSIGNMENT = "ASSIGNMENT";

    /** How much of the comment to quote in the notification body. */
    private static final int PREVIEW_LENGTH = 140;

    private final ClassroomCommentRepository commentRepository;
    private final ClassroomRepository classroomRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ClassroomStudentRepository classroomStudentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MinioService minioService;


    @Override
    public List<CommentResponse> getClassroomComments(UUID classroomId) {
        Classroom classroom = findClassroom(classroomId);
        User me = getCurrentUser();
        requireMember(classroom, me);

        return assemble(commentRepository.findTopLevelForClassroom(classroomId), classroom, me);
    }

    @Override
    public List<CommentResponse> getAssignmentComments(UUID assignmentId) {
        Assignment assignment = findAssignment(assignmentId);
        Classroom classroom = requireAssignmentClassroom(assignment);
        User me = getCurrentUser();
        requireMember(classroom, me);

        return assemble(commentRepository.findTopLevelForAssignment(assignmentId), classroom, me);
    }

    private List<CommentResponse> assemble(List<ClassroomComment> roots, Classroom classroom, User me) {
        if (roots.isEmpty()) {
            return List.of();
        }

        List<UUID> rootIds = roots.stream().map(ClassroomComment::getCommentId).toList();
        Map<UUID, List<ClassroomComment>> repliesByParent = commentRepository.findRepliesOf(rootIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getCommentId()));

        Map<UUID, ClassroomRole> roles = roleLookup(classroom);

        return roots.stream()
                .map(root -> toResponse(
                        root,
                        repliesByParent.getOrDefault(root.getCommentId(), List.of()),
                        me,
                        roles))
                .toList();
    }

    @Override
    public List<MentionUserResponse> getMentionableMembers(UUID classroomId, String query) {
        Classroom classroom = findClassroom(classroomId);
        User me = getCurrentUser();
        requireMember(classroom, me);

        Map<UUID, ClassroomRole> roles = roleLookup(classroom);
        String needle = query == null ? "" : query.trim().toLowerCase();

        return membersOf(classroom).stream()
                .filter(u -> !u.getId().equals(me.getId()))
                .filter(u -> needle.isEmpty() || matches(u, needle))
                .sorted(Comparator.comparing(u -> Optional.ofNullable(u.getFullName()).orElse("")))
                .map(u -> toMentionUser(u, roles.get(u.getId())))
                .toList();
    }

    @Override
    public List<MentionUserResponse> getMentionableMembersForAssignment(UUID assignmentId, String query) {
        Assignment assignment = findAssignment(assignmentId);
        return getMentionableMembers(requireAssignmentClassroom(assignment).getClassroomId(), query);
    }

    private boolean matches(User u, String needle) {
        return contains(u.getFullName(), needle)
                || contains(u.getNameKhmer(), needle)
                || contains(u.getEmail(), needle);
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }


    @Override
    @Transactional
    public CommentResponse createClassroomComment(UUID classroomId, CommentCreateRequest request) {
        return create(findClassroom(classroomId), null, request);
    }

    @Override
    @Transactional
    public CommentResponse createAssignmentComment(UUID assignmentId, CommentCreateRequest request) {
        Assignment assignment = findAssignment(assignmentId);
        return create(requireAssignmentClassroom(assignment), assignment, request);
    }

    private CommentResponse create(Classroom classroom, Assignment assignment,
                                   CommentCreateRequest request) {
        User author = getCurrentUser();
        requireMember(classroom, author);

        ClassroomComment comment = new ClassroomComment();
        comment.setClassroom(classroom);
        comment.setAssignment(assignment);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());
        comment.setCreatedAt(LocalDateTime.now());

        // Flatten deeper nesting: replying to a reply attaches to its root.
        ClassroomComment parent = null;
        if (request.parentId() != null) {
            parent = findComment(request.parentId());
            if (!parent.getClassroom().getClassroomId().equals(classroom.getClassroomId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Parent comment belongs to a different classroom");
            }
            UUID parentAssignmentId = parent.getAssignment() == null
                    ? null : parent.getAssignment().getAssignmentId();
            UUID thisAssignmentId = assignment == null ? null : assignment.getAssignmentId();
            if (!java.util.Objects.equals(parentAssignmentId, thisAssignmentId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot reply across the classroom wall and an assignment thread");
            }
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
            comment.setParent(parent);
        }

        ClassroomComment saved = commentRepository.save(comment);

        Set<User> mentioned = attachMentions(saved, classroom, request.mentionedUserIds());
        commentRepository.save(saved);

        notifyMentions(saved, classroom, author, mentioned);
        notifyParentAuthor(saved, parent, classroom, author, mentioned);

        Map<UUID, ClassroomRole> roles = roleLookup(classroom);
        return toResponse(saved, List.of(), author, roles);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentUpdateRequest request) {
        ClassroomComment comment = findComment(commentId);
        User me = getCurrentUser();
        Classroom classroom = comment.getClassroom();
        requireMember(classroom, me);

        if (!comment.getAuthor().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only edit your own comments");
        }

        Set<UUID> previouslyMentioned = comment.getMentions().stream()
                .map(m -> m.getMentionedUser().getId())
                .collect(Collectors.toSet());

        comment.setBody(request.body().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.getMentions().clear();

        Set<User> mentioned = attachMentions(comment, classroom, request.mentionedUserIds());
        ClassroomComment saved = commentRepository.save(comment);

        Set<User> newlyMentioned = mentioned.stream()
                .filter(u -> !previouslyMentioned.contains(u.getId()))
                .collect(Collectors.toSet());
        notifyMentions(saved, classroom, me, newlyMentioned);

        List<ClassroomComment> replies = saved.getParent() == null
                ? commentRepository.findRepliesOf(List.of(saved.getCommentId()))
                : List.of();

        return toResponse(saved, replies, me, roleLookup(classroom));
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        ClassroomComment comment = findComment(commentId);
        User me = getCurrentUser();
        Classroom classroom = comment.getClassroom();
        requireMember(classroom, me);

        boolean isAuthor = comment.getAuthor().getId().equals(me.getId());
        if (!isAuthor && !canModerate(classroom, me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the author or the classroom teacher can delete this comment");
        }


        commentRepository.delete(comment);
    }


    private Set<User> attachMentions(ClassroomComment comment, Classroom classroom, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }

        Map<UUID, User> membersById = membersOf(classroom).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Set<User> attached = new LinkedHashSet<>();
        for (UUID id : new LinkedHashSet<>(userIds)) {
            if (id == null || id.equals(comment.getAuthor().getId())) {
                continue;
            }
            User target = membersById.get(id);
            if (target == null) {
                // Someone edited the payload, or the member was removed between
                // opening the composer and posting. Skip rather than 400 — the
                // comment itself is still valid.
                log.debug("Ignoring mention of non-member {} in classroom {}",
                        id, classroom.getClassroomId());
                continue;
            }
            comment.getMentions().add(new CommentMention(comment, target));
            attached.add(target);
        }
        return attached;
    }

    private void notifyMentions(ClassroomComment comment, Classroom classroom,
                                User author, Set<User> mentioned) {
        for (User target : mentioned) {
            notificationService.createNotification(
                    target.getId(),
                    "You were mentioned",
                    "%s mentioned you in %s: \"%s\"".formatted(
                            displayName(author), classroom.getClassName(), preview(comment.getBody())),
                    TYPE_MENTION,
                    contextLabel(classroom, comment),
                    displayName(author),
                    linkFor(target, classroom, comment),
                    comment.getAssignment() != null ? RESOURCE_ASSIGNMENT : RESOURCE_CLASSROOM,
                    comment.getAssignment() != null
                            ? comment.getAssignment().getAssignmentId()
                            : classroom.getClassroomId()
            );
        }
    }

    private void notifyParentAuthor(ClassroomComment reply, ClassroomComment parent,
                                    Classroom classroom, User author, Set<User> alreadyNotified) {
        if (parent == null) {
            return;
        }
        User parentAuthor = parent.getAuthor();
        if (parentAuthor.getId().equals(author.getId())) {
            return; // replying to yourself
        }
        if (alreadyNotified.stream().anyMatch(u -> u.getId().equals(parentAuthor.getId()))) {
            return; // already got the mention notification for this same comment
        }

        notificationService.createNotification(
                parentAuthor.getId(),
                "New reply to your comment",
                "%s replied in %s: \"%s\"".formatted(
                        displayName(author), classroom.getClassName(), preview(reply.getBody())),
                TYPE_REPLY,
                contextLabel(classroom, reply),
                displayName(author),
                linkFor(parentAuthor, classroom, reply),
                reply.getAssignment() != null ? RESOURCE_ASSIGNMENT : RESOURCE_CLASSROOM,
                reply.getAssignment() != null
                        ? reply.getAssignment().getAssignmentId()
                        : classroom.getClassroomId()
        );
    }

    /** Teachers and students open the same classroom at different routes. */
    private String linkFor(User recipient, Classroom classroom, ClassroomComment comment) {
        boolean isTeacher = isClassroomTeacher(classroom, recipient)
                || hasClassroomRole(classroom, recipient, ClassroomRole.TEACHER);

        if (comment.getAssignment() != null) {
            UUID assignmentId = comment.getAssignment().getAssignmentId();
            return isTeacher
                    ? TEACHER_ASSIGNMENT_ROUTE.formatted(assignmentId, comment.getCommentId())
                    : STUDENT_ASSIGNMENT_ROUTE.formatted(
                    classroom.getClassroomId(), assignmentId, comment.getCommentId());
        }

        String template = isTeacher ? TEACHER_CLASSROOM_ROUTE : STUDENT_CLASSROOM_ROUTE;
        return template.formatted(classroom.getClassroomId(), comment.getCommentId());
    }

    private String preview(String body) {
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH) + "…";
    }


    private Set<User> membersOf(Classroom classroom) {
        Set<User> users = new LinkedHashSet<>();

        if (classroom.getTeacher() != null && classroom.getTeacher().getUser() != null) {
            users.add(classroom.getTeacher().getUser());
        }

        for (ClassroomStudent cs : classroomStudentRepository
                .findByClassroom_ClassroomId(classroom.getClassroomId())) {
            if (cs.getStudent() != null && cs.getStudent().getUser() != null) {
                users.add(cs.getStudent().getUser());
            }
        }

        for (ClassroomMember cm : classroomMemberRepository
                .findByClassroom_ClassroomId(classroom.getClassroomId())) {
            if (cm.getStatus() == MemberStatus.ACTIVE && cm.getUser() != null) {
                users.add(cm.getUser());
            }
        }

        return users;
    }

    private Map<UUID, ClassroomRole> roleLookup(Classroom classroom) {
        Map<UUID, ClassroomRole> roles = new HashMap<>();

        for (ClassroomStudent cs : classroomStudentRepository
                .findByClassroom_ClassroomId(classroom.getClassroomId())) {
            if (cs.getStudent() != null && cs.getStudent().getUser() != null) {
                roles.put(cs.getStudent().getUser().getId(), ClassroomRole.STUDENT);
            }
        }
        for (ClassroomMember cm : classroomMemberRepository
                .findByClassroom_ClassroomId(classroom.getClassroomId())) {
            if (cm.getUser() != null && cm.getRole() != null) {
                roles.put(cm.getUser().getId(), cm.getRole());
            }
        }
        if (classroom.getTeacher() != null && classroom.getTeacher().getUser() != null) {
            roles.put(classroom.getTeacher().getUser().getId(), ClassroomRole.TEACHER);
        }
        return roles;
    }

    private void requireMember(Classroom classroom, User user) {
        if (isPlatformAdmin() || isMember(classroom, user)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not a member of this classroom");
    }

    private boolean isMember(Classroom classroom, User user) {
        if (isClassroomTeacher(classroom, user)) {
            return true;
        }
        if (classroomMemberRepository.existsByClassroom_ClassroomIdAndUser_Id(
                classroom.getClassroomId(), user.getId())) {
            return true;
        }
        return membersOf(classroom).stream().anyMatch(u -> u.getId().equals(user.getId()));
    }

    /** The classroom's teacher, or a platform admin, may remove anyone's comment. */
    private boolean canModerate(Classroom classroom, User user) {
        return isPlatformAdmin()
                || isClassroomTeacher(classroom, user)
                || hasClassroomRole(classroom, user, ClassroomRole.TEACHER);
    }

    private boolean isClassroomTeacher(Classroom classroom, User user) {
        return classroom.getTeacher() != null
                && classroom.getTeacher().getUser() != null
                && classroom.getTeacher().getUser().getId().equals(user.getId());
    }

    private boolean hasClassroomRole(Classroom classroom, User user, ClassroomRole role) {
        return classroomMemberRepository.existsByClassroom_ClassroomIdAndUser_IdAndRoleAndStatus(
                classroom.getClassroomId(), user.getId(), role, MemberStatus.ACTIVE);
    }

    private boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }


    private CommentResponse toResponse(ClassroomComment c,
                                       List<ClassroomComment> replies,
                                       User me,
                                       Map<UUID, ClassroomRole> roles) {
        boolean isAuthor = c.getAuthor().getId().equals(me.getId());

        List<MentionUserResponse> mentions = c.getMentions().stream()
                .map(m -> toMentionUser(m.getMentionedUser(), roles.get(m.getMentionedUser().getId())))
                .toList();

        List<CommentResponse> replyResponses = replies.stream()
                .map(r -> toResponse(r, List.of(), me, roles))
                .toList();

        return new CommentResponse(
                c.getCommentId(),
                c.getClassroom().getClassroomId(),
                c.getAssignment() != null ? c.getAssignment().getAssignmentId() : null,
                c.getParent() != null ? c.getParent().getCommentId() : null,
                c.getBody(),
                c.isEdited(),
                c.getAuthor().getId(),
                displayName(c.getAuthor()),
                roleName(roles.get(c.getAuthor().getId())),
                avatarUrl(c.getAuthor()),
                mentions,
                replyResponses,
                isAuthor,
                isAuthor || canModerate(c.getClassroom(), me),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private MentionUserResponse toMentionUser(User u, ClassroomRole role) {
        return new MentionUserResponse(
                u.getId(),
                displayName(u),
                u.getNameKhmer(),
                u.getEmail(),
                roleName(role),
                avatarUrl(u)
        );
    }

    private String roleName(ClassroomRole role) {
        return role == null ? ClassroomRole.STUDENT.name() : role.name();
    }

    private String displayName(User u) {
        if (u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName();
        }
        return u.getEmail();
    }

    /** Never let a MinIO hiccup break the whole thread — the avatar is decoration. */
    private String avatarUrl(User u) {
        if (u.getAvatarObjectName() == null) {
            return null;
        }
        try {
            return minioService.getAssetPreviewUrl(u.getAvatarObjectName());
        } catch (Exception e) {
            return null;
        }
    }


    private String contextLabel(Classroom classroom, ClassroomComment comment) {
        if (comment.getAssignment() != null) {
            return "%s · %s".formatted(classroom.getClassName(), comment.getAssignment().getTitle());
        }
        return classroom.getClassName();
    }

    private Assignment findAssignment(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assignment not found: " + assignmentId));
    }


    private Classroom requireAssignmentClassroom(Assignment assignment) {
        if (assignment.getClassroom() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This assignment is not attached to a classroom, so it has no discussion");
        }
        return assignment.getClassroom();
    }

    private Classroom findClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Classroom not found: " + classroomId));
    }

    private ClassroomComment findComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comment not found: " + commentId));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new UserNotFoundException();
        }
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(UserNotFoundException::new);
    }
}