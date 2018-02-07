package org.fpwei.game.server.service;

import org.fpwei.game.server.entity.User;
import org.fpwei.game.server.message.LoginRequest;

public interface UserService {
    User login(LoginRequest request) ;
}
