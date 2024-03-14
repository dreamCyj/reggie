package com.example.reggie.common;


import com.example.reggie.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j

/**
 * 全局异常处理器
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)//捕获所有异常
    //@ExceptionHandler(SQLIntegrityConstraintViolationException.class)//捕获这种异常 spilt[2]
    public Result<String> ex(Exception ex){
        log.error(ex.getMessage());
        if(ex.getMessage().contains("Duplicate entry")) {
            String[] spilt = ex.getMessage().split(" ");
            String msg = spilt[9] + "已存在";
            return Result.error(msg);
         }
        return Result.error("sorry");
    }

    @ExceptionHandler(CustomException.class)
    public Result<String> ex(CustomException ex){
        log.error(ex.getMessage());
        return Result.error(ex.getMessage());
    }
}

