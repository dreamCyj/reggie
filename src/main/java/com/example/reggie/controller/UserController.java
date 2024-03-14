package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.reggie.common.Result;
import com.example.reggie.entity.User;
import com.example.reggie.service.UserService;
import com.example.reggie.utils.SMSUtils;
import com.example.reggie.utils.ValidateCodeUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/sendMsg")
    public Result<String> sendMsg(HttpSession session, @RequestBody User user){
        //获取手机号
        String phone = user.getPhone();
        //生成验证码
        if(phone != null){
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("code:{}",code);
            //调用阿里云短信服务api发送短信
            //SMSUtils.sendMessage("阿里云短信测试","SMS_154950909", phone, code);
            //验证码保存到session，以便校验
            session.setAttribute(phone,code);
            return Result.success("验证码已发送");
        }
        return Result.error("验证码发送失败");
    }
    @PostMapping("/login")
    public Result<User> login(HttpSession session, @RequestBody Map map){
        //获取手机号和验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();
        //从session中获取验证码
        Object codeInSession = session.getAttribute(phone);
        //比对
        if(codeInSession != null && codeInSession.equals(code)){
            //当前用户是否存在 不存在则注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if(user == null){
                //新用户
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                user.setName("user_"+ IdWorker.getId());
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            return Result.success(user);
        }
        return Result.error("登录失败");
    }
}
