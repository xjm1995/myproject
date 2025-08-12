//package com.zj.xjm.controller;
//
//import com.zj.xjm.mapper.UserMapper;
//import com.zj.xjm.pojo.User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServletRequest;
//
//@Controller
//public class index {
//    @Autowired
//    private UserMapper userMapper;
//
//
//    @GetMapping("/")
//    public String index(HttpServletRequest request) {
//        Cookie[] cookies = request.getCookies();
//        for (Cookie cookie : cookies) {
//            if (cookie.getName().equals("token")) {
//                String token = cookie.getValue();
//                User user = userMapper.findbyToken(token);
//                if (user != null) {
//                    request.getSession().setAttribute("user", user);
//                }
//                break;
//            }
//        }
//        return "index";
//    }
//}
