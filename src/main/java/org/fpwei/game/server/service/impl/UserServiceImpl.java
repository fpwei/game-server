package org.fpwei.game.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.fpwei.game.server.dao.UserDao;
import org.fpwei.game.server.entity.User;
import org.fpwei.game.server.message.LoginRequest;
import org.fpwei.game.server.service.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final int DEFAULT_AMOUNT = 2000;

//    @Autowired
    private UserDao userDao;

    @Override
    public User login(LoginRequest request) {
        User user;

        switch (request.getType()) {
            case 0:
                user = new User();
                user.setAmount(new BigDecimal(DEFAULT_AMOUNT));
                user.setName(RandomStringUtils.randomAlphanumeric(10));
                break;
            case 1:

                user = userDao.getUser(request.getAccount(), request.getPassword());
                break;
            default:
                throw new IllegalArgumentException();
        }

        return user;
    }
}
