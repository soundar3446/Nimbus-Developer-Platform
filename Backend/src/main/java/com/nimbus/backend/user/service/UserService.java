package com.nimbus.backend.user.service;

import com.nimbus.backend.user.dto.UserRequest;
import com.nimbus.backend.user.dto.UserResponse;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUser(Long id);

}
