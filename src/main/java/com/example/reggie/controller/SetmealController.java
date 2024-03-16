package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.Result;
import com.example.reggie.dto.DishDto;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Dish;
import com.example.reggie.entity.Setmeal;
import com.example.reggie.entity.SetmealDish;
import com.example.reggie.service.CategoryService;
import com.example.reggie.service.SetmealDishService;
import com.example.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetmealDishService setmealDishService;

    @GetMapping("/page")
    public Result<Page<SetmealDto>> page(int page, int pageSize, String name){
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(name != null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, queryWrapper);
        //到这一步除了套餐分类这一栏都展示成功 前端要的setmealName，但是这里只有setmealId 后面由id得到name进行展示
        /**
         * 要改pageInfo中的records中每个item的套餐分类这一栏,就是每一行数据的套餐分类
         * 所以先拷贝除了records中的所有,然后把records单独拿出来处理.我们要的categoryName在setmealDto里，所以需要new一个并进行操作得到categoryName
         * 但是这个setmealDto里除了categoryName啥也没有，所以还要将item里的东西再拷贝给setmealDto
         * 这样setmealDto就是有categoryName的item，再收集起来形成新的records(有categoryName的)放进setmealDtoPage返回给前端
         * pageInfo-->records-->item
         * item + categoryName -->setmealDto(有categoryName属性)
         * setmealDtoPage<--records<--setmealDto
         */
        //对象拷贝
        Page<SetmealDto> setmealDtoPage = new Page<>();
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");//把pageInfo中除了records的全部拷贝给dishDtoPage
        List<Setmeal> records = pageInfo.getRecords();//records为页面展示的菜品列表，需要单独处理
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            Long categoryId = item.getCategoryId();//拿到每个item套餐的分类id
            //根据id到category表查询菜品名称
            Category category = categoryService.getById(categoryId);//根据id查到当前菜品种类
            if(category != null){
                setmealDto.setCategoryName(category.getName());//拿到菜品名称,将其赋值给dishDto
            }
            return setmealDto;
        }).toList();
        setmealDtoPage.setRecords(list);//records单独处理完成
        return Result.success(setmealDtoPage);
    }

    /**
     * 新增套餐 不仅要将套餐基本信息插入setmeal表，还要将套餐内的菜品与套餐的关联信息插入到setmeal_dish表 菜品要绑定套餐id
     * 与新增菜品 要添加口味信息一样 口味要绑定菜品id
     * @param setmealDto
     * @return
     */
    @PostMapping()
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> save(@RequestBody SetmealDto setmealDto){//前端传过来的是JSON，要加@RequestBody
        setmealService.saveWithDishes(setmealDto);
        return Result.success("套餐添加成功");
    }

    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> delete(@RequestParam List<Long> ids){
        //Spring MVC获取参数不带注解的唯一要求就是参数名和Http请求参数名一致。这里前端传的是ids=176777...,141558...逗号分割而不是像之前page=1&pageSize=10，要加@RequestParam
        log.info("ids:{}", ids);
        setmealService.deleteWithDishes(ids);
        return Result.success("套餐删除成功");
    }
    @GetMapping("/{id}")
    public Result<SetmealDto> get(@PathVariable Long id){
        return Result.success(setmealService.getByIdWithDishes(id));
    }
    @PutMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDishes(setmealDto);
        return Result.success("套餐修改成功");
    }
    @PostMapping("/status/0")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> disable(@RequestParam List<Long> ids){
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Setmeal::getId, ids);
        updateWrapper.set(Setmeal::getStatus, 0);
        setmealService.update(updateWrapper);
        log.info("停售套餐");
        return Result.success("套餐已停售");
    }

    @PostMapping("/status/1")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> enable(@RequestParam List<Long> ids){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(1);
        setmealService.update(setmeal,queryWrapper);
        log.info("启售套餐");
        return Result.success("套餐已启售");
    }
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public Result<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return Result.success(setmealList);
    }
    @GetMapping("/dish/{id}")
    public Result<Setmeal> getById(@PathVariable Long id){
        return Result.success(setmealService.getById(id));
    }

}
