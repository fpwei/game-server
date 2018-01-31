package org.fpwei.game.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fpwei.game.server.common.CommonRuntimeException;
import org.fpwei.game.server.dao.UserDao;
import org.fpwei.game.server.entity.User;
import org.fpwei.game.server.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserDao userDao;

    @Override
    public User login(String[] arr) {
        //GameID,LOGINKEY,GameType
        if (arr.length != 3) {
            log.debug("長度不對");
            throw new CommonRuntimeException("長度不對");
        }

        String gameId = arr[0];
        String loginKey = arr[1];
        String gameType = arr[2];

        if (!StringUtils.isNumeric(gameId)) {
            log.debug("封包錯誤 遊戲ID應該是數字");
            throw new CommonRuntimeException("封包錯誤 遊戲ID應該是數字");
        }

        if (StringUtils.isBlank(loginKey)) {
            log.debug("封包錯誤 沒有登入碼");
            throw new CommonRuntimeException("封包錯誤 沒有登入碼");
        }

        if (!StringUtils.isNumeric(gameType)) {
            log.debug("封包錯誤 遊戲類別應該是數字");
            throw new CommonRuntimeException("封包錯誤 遊戲類別應該是數字");
        }


        /*
TODO
this.playID = Integer.parseInt(arr[1]);
ArrayList<HashMap> dbArr = MysqlDB.getEntity().selectTable("select a1.nickName,a2.money from game_user as a1,user_game_money as a2 where a1.id=a2.user_id and a1.id ='" + playID + "'", new String[]{"nickName", "money"}, new int[]{MysqlDB.TABLE_STRING, MysqlDB.TABLE_INT});
if (dbArr.size() != 0) {
HashMap set = dbArr.get(0);
String nickName = (String) set.get("nickName");
String money = String.valueOf(set.get("money"));
send("6!@#" + money + "," + nickName);
Happy100.getMain().join(this);
}
*/


        User user = userDao.getPlayerById(loginKey);

        return user;
    }
}
