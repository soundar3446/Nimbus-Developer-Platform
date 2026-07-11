package com.nimbus.backend.user.service.impl;

import com.nimbus.backend.common.exception.AlreadyExistsException;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.user.dto.UserRequest;
import com.nimbus.backend.user.dto.UserResponse;
import com.nimbus.backend.user.entity.User;
import com.nimbus.backend.user.mapper.UserMapper;
import com.nimbus.backend.user.repository.UserRepository;
import com.nimbus.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {

        //Checking whether the email exists already
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("A user with this email already exists: " + request.getEmail());
        }

        // 1. Map DTO to Entity cleanly
        User user = userMapper.toEntity(request);

        // 2. Save to database
        User savedUser = userRepository.save(user);

        // 3. Return mapped response DTO
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        // 1. Fetch user by ID or throw exception if not found
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // 2. Return mapped response DTO
        return userMapper.toResponse(user);
    }

}