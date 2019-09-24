package com.why.springbootpractice.controllers;

import com.why.springbootpractice.pojos.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wanghaiyang on 2019/9/16.
 */
@RestController
public class EnvCheckController {
    @Autowired
    private Resource resource;

    @RequestMapping("/envcheck")
    public Object EnvCheck() {
        return "Environment checked fine.";
    }

    @RequestMapping("/getresource")
    public Resource getResource() {
        Resource bean = new Resource();
        BeanUtils.copyProperties(resource, bean);
        return bean;
    }
}
