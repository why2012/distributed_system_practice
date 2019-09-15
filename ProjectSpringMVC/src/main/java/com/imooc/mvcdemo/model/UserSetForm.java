package com.imooc.mvcdemo.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by wanghaiyang on 2019/8/20.
 */
public class UserSetForm {
    private Set<User> users;

    public UserSetForm() {
        users = new LinkedHashSet<User>();
        users.add(new User(null, 1));
        users.add(new User(null,2));
        users.add(new User(null, 3));
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
