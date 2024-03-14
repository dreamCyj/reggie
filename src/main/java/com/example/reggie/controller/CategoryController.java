package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.Result;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Emp;
import com.example.reggie.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    //新增分类
    @PostMapping
    public Result<String> addCategory(@RequestBody Category category){
        log.info("新增分类：{}", category);
        categoryService.save(category);
        return Result.success("新增分类成功:");
    }
    //分页查询
    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize){
        log.info("分页查询参数：{}, {}", page, pageSize);
        //构造分页构造器
        Page pageInfo = new Page(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //添加排序条件
        queryWrapper.orderByAsc(Category::getSort);
        //执行查询
        categoryService.page(pageInfo, queryWrapper);
        return Result.success(pageInfo);
    }
    //根据ID删除
    @DeleteMapping
    public Result<String> delete(Long id){
        log.info("删除分类：{}", id);
        //categoryService.removeById(id);
        categoryService.remove(id);
        return Result.success("删除成功");
    }
/*    //根据ID查询(回显) 前端已经实现
    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id){
        return Result.success(categoryService.getById(id));
    }*/
    //根据ID修改
    @PutMapping
    public Result<String> update(@RequestBody Category category){
        log.info("修改分类：{}", category);
        categoryService.updateById(category);
        return Result.success("修改成功");
    }

    //根据id查询菜品分类 用于新增菜品时的下拉框选择
    @GetMapping("/list")
    public Result<List<Category>> list(Category category){ //前端传过来的category只有一个属性 就是type：1
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType()); //从Category表中找type为category.getType()的 即找type为1的分类 即菜品分类
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        List<Category> list = categoryService.list(queryWrapper);
        return Result.success(list);
    }
}
