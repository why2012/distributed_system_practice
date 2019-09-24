package com.why.springbootpractice.controllers;

import com.why.springbootpractice.pojos.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by wanghaiyang on 2019/9/16.
 */
@Controller
public class UserController {

    @RequestMapping("/userpojo")
    @ResponseBody
    public User userPojo() {
        Calendar c = Calendar.getInstance();
        c.set(c.YEAR, 1993);
        c.set(c.MONTH, 10);
        c.set(c.DAY_OF_MONTH, 19);
        User user = new User();
        user.setName("Ethan");
        user.setAge(23);
        user.setBirthday(c.getTime());
        user.setPassword("123456");
        user.setDesc("This is Ethan's user pojo");
        return user;
    }
}
