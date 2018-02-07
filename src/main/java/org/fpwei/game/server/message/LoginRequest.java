package org.fpwei.game.server.message;

import org.fpwei.game.server.annotation.Message;

@Message("login")
public class LoginRequest {
    private int type;

    private String account;

    private String password;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
