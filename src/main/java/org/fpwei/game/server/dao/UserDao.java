package org.fpwei.game.server.dao;

import org.fpwei.game.server.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao {
    User getPlayerById(String id);

    User getUser(String account, String password);
}
