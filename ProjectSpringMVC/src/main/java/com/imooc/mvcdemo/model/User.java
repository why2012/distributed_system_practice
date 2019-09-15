package com.imooc.mvcdemo.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Created by wanghaiyang on 2019/8/19.
 */
@XmlRootElement(name = "user")
public class User {

    private String name;
    private Integer age;
    private ContactInfo contactInfo;

    public User() {}

    public User(String userName, Integer userAge) {
        name = userName;
        age = userAge;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setName(String name) {

        this.name = name;
    }

    @XmlElement(name = "age")
    public Integer getAge() {

        return age;
    }

    @XmlElement(name = "name")
    public String getName() {

        return name;
    }

    @XmlElement(name = "contactInfo")
    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name) && Objects.equals(age, user.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
