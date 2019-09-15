package com.why.springbootpractice.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by wanghaiyang on 2019/9/16.
 */
@RestController
public class EnvCheckController {

    @RequestMapping("/envcheck")
    public Object EnvCheck() {
        return "Environment checked fine.";
    }
}
