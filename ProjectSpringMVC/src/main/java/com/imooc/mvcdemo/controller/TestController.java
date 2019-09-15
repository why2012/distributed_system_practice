package com.imooc.mvcdemo.controller;

import com.imooc.mvcdemo.model.User;
import com.imooc.mvcdemo.model.UserListForm;
import com.imooc.mvcdemo.model.UserMapForm;
import com.imooc.mvcdemo.model.UserSetForm;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.format.number.CurrencyFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wanghaiyang on 2019/8/19.
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @RequestMapping(value="/{courseId}",method = RequestMethod.GET)
    @ResponseBody
    public Integer getCourseInJson(@PathVariable("courseId") Integer courseId){
        return courseId ^ 2;
    }

    @RequestMapping(value="/array",method = RequestMethod.GET)
    @ResponseBody
    // http://127.0.0.1:8888/test/array?name=a&name=b
    public String getArrayJson(@RequestParam("name") String[] strlist){
        StringBuffer stringBuffer = new StringBuffer();
        for (String str : strlist) {
            stringBuffer.append(str).append("  ");
        }
        return stringBuffer.toString();
    }

    @RequestMapping(value="/object",method = RequestMethod.GET)
    @ResponseBody
    public User getObjectJson(User user){
        return user;
    }

    @RequestMapping(value="/object2",method = RequestMethod.GET)
    @ResponseBody
    //http://127.0.0.1:8888/test/object2?user.name=why&admin.name=tom&age=26&user.contactInfo.phone=10086
    public User[] getObject2Json(@ModelAttribute("user") User user, @ModelAttribute("admin") User admin){
        return new User[]{user, admin};
    }

    @InitBinder("user")
    public void initUserBinder(WebDataBinder userBinder) {
        userBinder.setFieldDefaultPrefix("user.");
    }

    @InitBinder("admin")
    public void initAdminBinder(WebDataBinder userBinder) {
        userBinder.setFieldDefaultPrefix("admin.");
    }

    @RequestMapping(value="/list",method = RequestMethod.GET)
    @ResponseBody
    //http://localhost:8080/test/list?users[0].name=why&users[1].name=tom
    public UserListForm getListJson(UserListForm users){
        return users;
    }

    @RequestMapping(value="/set",method = RequestMethod.GET)
    @ResponseBody
    //http://localhost:8080/test/set?users[0].name=why&users[1].name=tom&users[2].name=why&users[2].age=1
    public UserSetForm getSetJson(UserSetForm users) {
        return users;
    }

    @RequestMapping(value="/map",method = RequestMethod.GET)
    @ResponseBody
    //http://localhost:8080/test/map?users[why].name=why&users[tom].name=tom&users[lucy].name=lucy
    public UserMapForm getMapJson(UserMapForm users) {
        return users;
    }

    @RequestMapping(value = "/json", method = RequestMethod.POST)
    @ResponseBody
    //http://localhost:8080/test/json
//    {
//        "name": "why",
//        "age": 26,
//        "contactInfo": {
//          "phone": "1111111",
//          "address": "4th street"
//        }
//    }
    public User setUserJson(@RequestBody User user) {
        return user;
    }

    @RequestMapping(value = "/xml", method = RequestMethod.POST, produces = {"application/json; charset=UTF-8"})
    @ResponseBody
    //http://localhost:8080/test/xml
    //http://localhost:8080/test/xml.xml
    //http://localhost:8080/test/xml.json
//    <?xml version="1.0" encoding="UTF-8"?>
//    <user>
//        <name>why</name>
//        <age>26</age>
//        <contactInfo>
//            <phone>1111111</phone>
//            <address>4th street</address>
//        </contactInfo>
//    </user>
    public User setUserXml(@RequestBody User user) {
        return user;
    }

    @RequestMapping(value="/stringToBooleanConverter",method = RequestMethod.GET)
    @ResponseBody
    public String stringToBoolean(Boolean bool){
        //CustomDateEditor customDateEditor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), false);
        //CurrencyFormatter currencyFormatter = new CurrencyFormatter();
        //StringToBooleanConverter stringToBooleanConverter = new StringToBooleanConverter();

        return bool.toString();
    }

    @RequestMapping(value="/date",method = RequestMethod.GET)
    @ResponseBody
    public String getDateJson(Date myDate, Date myDate2) {
        return myDate.toString() + " " + myDate2.toString();
    }

    @InitBinder(value = {"myDate", "myDate2"})
    public void initDateBinder(WebDataBinder userBinder) {
        userBinder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }
}
