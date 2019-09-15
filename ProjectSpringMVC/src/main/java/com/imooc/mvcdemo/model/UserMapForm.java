package com.imooc.mvcdemo.model;

import java.util.Map;

/**
 * Created by wanghaiyang on 2019/8/20.
 */
public class UserMapForm {
    private Map<String, User> users;


    public Map<String, User> getUsers() {
        return users;
    }

    public void setUsers(Map<String, User> users) {
        this.users = users;
    }
}
