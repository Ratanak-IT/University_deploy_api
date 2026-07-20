package com.universitymanagement.classroom.entity;


import com.universitymanagement.classroom.dto.ClassroomRole;
import com.universitymanagement.classroom.dto.MemberStatus;
import com.universitymanagement.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "classroom_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_classroom_user",
                        columnNames = {
                                "classroom_id",
                                "user_id"
                        }
                )
        }
)
@Getter
@Setter
public class ClassroomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "classroom_id",
            nullable = false
    )
    private Classroom classroom;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassroomRole role;



    private LocalDateTime joinedAt = LocalDateTime.now();


    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.ACTIVE;
}
