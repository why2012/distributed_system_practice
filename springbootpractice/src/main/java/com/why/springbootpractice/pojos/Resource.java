package com.why.springbootpractice.pojos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by wanghaiyang on 2019/9/16.
 */
@Configuration
@ConfigurationProperties(prefix = "com.why.opensource")
@PropertySource(value = "classpath:resources.properties")
public class Resource {
    private String name;
    private String website;
    private String language;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
