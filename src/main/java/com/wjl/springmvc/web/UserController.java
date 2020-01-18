package com.wjl.springmvc.web;

import com.wjl.springmvc.annatation.MyController;
import com.wjl.springmvc.annatation.MyRequestMapping;

@MyController
public class UserController {

    public UserController() {
        System.out.println("初始化控制器类UserController");
    }

    @MyRequestMapping(value = "/user")
    public String sayHello(){
        return "index";
    }
}
