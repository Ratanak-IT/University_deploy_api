package com.universitymanagement.classroom.service.impl;

import com.universitymanagement.classroom.mapper.ClassroomMapper;
import com.universitymanagement.classroom.repository.ClassroomRepository;
import com.universitymanagement.classroom.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMapper classroomMapper;

}
