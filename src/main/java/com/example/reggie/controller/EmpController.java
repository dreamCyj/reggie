package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.Result;
import com.example.reggie.entity.Emp;
import com.example.reggie.service.EmpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * employee-->emp  axios中url | 实体 | controller.service.mapper中的<> |数据库中的表 | @RequestMapping都要进行相应修改
 */
@Slf4j
@RestController
@RequestMapping("/emp")
public class EmpController {

    @Autowired
    private EmpService empService;
    //登录
    @PostMapping("/login")
    public Result<Emp> login(HttpServletRequest request, @RequestBody Emp emp) {
        //1.将页面提交的密码password进行md5加密处理
        String password = emp.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        //2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Emp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Emp::getUsername, emp.getUsername()); //查询条件
        Emp e = empService.getOne(queryWrapper);
        //3.查询不到则返回登录失败结果
        if(e == null){return Result.error("登陆失败!");}
        //4.密码比对，不一致同样返回登录失败
        if(!e.getPassword().equals(password)){return Result.error("登陆失败!");}
        //5.查看员工状态,已禁用则返回员工已禁用结果
        if(e.getStatus() == 0){
            return Result.error("账号已被禁用!");
        }
        //6.登录成功，将员工id存入session并返回登录成功结果
        request.getSession().setAttribute("emp", e.getId());
        return Result.success(e);
    }
    //退出
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request){
        //清理session中保存的当前登录员工ID
        request.getSession().removeAttribute("emp");
        return Result.success("退出成功");
    }
    //添加员工
    @PostMapping
    public Result<String> addEmp(HttpServletRequest request, @RequestBody Emp emp){
        log.info("添加员工：{}", emp);
        emp.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
/*        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        Long empId = (Long) request.getSession().getAttribute("emp");
        emp.setCreateUser(empId);
        emp.setUpdateUser(empId);*/
        empService.save(emp);
        return Result.success("成功添加员工:"+emp.getUsername());
    }
    //分页条件查询
    @GetMapping("/page")
    public Result<Page<Emp>> page(int page, int pageSize, String name){
        log.info("分页查询参数：{}, {}, {}", page, pageSize, name);
        //构造分页构造器
        Page<Emp> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Emp> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Emp::getName, name);
        queryWrapper.ne(Emp::getId, 1);//排除管理员
        //添加排序条件
        queryWrapper.orderByDesc(Emp::getUpdateTime);
        //执行查询
        empService.page(pageInfo, queryWrapper);

        return Result.success(pageInfo);
    }
    //根据ID修改员工
    @PutMapping //无论是编辑还是禁用都是PUT访问/emp 都执行update方法
    public Result<String> update(@RequestBody Emp emp){ //参数是由前端传过来的
/*        emp.setUpdateTime(LocalDateTime.now());
        emp.setUpdateUser((Long) request.getSession().getAttribute("emp"));*/
        empService.updateById(emp); //empService继承IService IService自带updateById
        //前端js处理long型数据丢失精度 只能处理16位 多余的四舍五入 解决:扩展消息转换器，Long-->String
        return Result.success("员工信息修改成功");
    }
    //根据ID查询员工
    @GetMapping("/{id}")
    public Result<Emp> getById(@PathVariable Long id){
        log.info("根据ID查询员工信息");
        Emp emp = empService.getById(id);
        if(emp != null){
            return Result.success(emp);
        }
        return Result.error("未查询到员工信息");
    }
}
