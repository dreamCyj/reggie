package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.Result;
import com.example.reggie.dto.DishDto;
import com.example.reggie.entity.Category;
import com.example.reggie.entity.Dish;
import com.example.reggie.service.CategoryService;
import com.example.reggie.service.DishFlavorService;
import com.example.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/page")
    public Result<Page<DishDto>> page(int page, int pageSize, String name){
        log.info("分页查询参数：{}, {}, {}", page, pageSize, name);
        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize); //这里用DishDto不用Dish是因为Dish里面没有前端需要的catrgoryName,
        Page<DishDto> dishDtoPage = new Page<>();
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort);
        //执行查询
        dishService.page(pageInfo, queryWrapper);
        //到这一步除了菜品分类这一栏都展示成功 前端要的categoryName，但是这里只有categoryId 后面由id得到name进行展示
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");//把pageInfo中除了records的全部拷贝给dishDtoPage
        List<Dish> records = pageInfo.getRecords();//records为页面展示的菜品列表，需要单独处理
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();//拿到每个item菜品的分类id
            //根据id到category表查询菜品名称
            Category category = categoryService.getById(categoryId);//根据id查到当前菜品种类
            if(category != null){
                dishDto.setCategoryName(category.getName());//拿到菜品名称,将其赋值给dishDto
            }
            return dishDto;
        }).toList();

        dishDtoPage.setRecords(list);//records单独处理完成
        return Result.success(dishDtoPage);
    }
    //新增菜品
/*    1、页面(backend/page/food/add.htm)发送ajax请求，请求服务端获取菜品分类数据并展示到下拉框中  /category/list
    2、页面发送请求进行图片上传，请求服务端将图片保存到服务器
    3、页面发送请求进行图片下载，将上传的图片进行回显
    4、点击保存按钮，发送ajax请求，将菜品相关数据以)json形式提交到服务端*/
    //要操作两张表 dish和dish_flavor
    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto){//前端传过来的是JSON，要加@RequestBody
        dishService.saveWithFlavor(dishDto);
        //清理所有菜品缓存数据
/*        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        //清理某个分类的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return Result.success("菜品添加成功");
    }
    @GetMapping("/{id}")
    public Result<DishDto> get(@PathVariable Long id){
        return Result.success(dishService.getByIdWithFlavor(id));
    }


    @PutMapping
    public Result<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        //清理所有菜品缓存数据
/*        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        //清理某个分类的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return Result.success("菜品修改成功");
    }

/*    @GetMapping("/list")
    public Result<List<Dish>> getByCategory(Dish dish){ //前端传来的只有categoryId,但用dish实体更加全面，这里Long categoryId也行
        //根据传来的categoryId去dish表中找对应的菜品
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);//状态为1代表启售 而非停售
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        return Result.success(list);
    }*/

    @GetMapping("/list")
    public Result<List<DishDto>> getByCategory(Dish dish){ //前端传来的只有categoryId,但用dish实体更加全面，这里Long categoryId也行
        //先从redis中获取缓存数据，没有再执行数据库查询并缓存进redis
        List<DishDto> dishDtoList = null;
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        log.info("菜品信息Key:{}", key);
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if(dishDtoList != null){
            return Result.success(dishDtoList);
        }else {
            //不存在则进行查询并存入缓存
            //根据传来的categoryId去dish表中找对应的菜品
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
            queryWrapper.eq(Dish::getStatus,1);//状态为1代表启售 而非停售
            queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
            List<Dish> list = dishService.list(queryWrapper);//这里没有口味数据 接下来为每个dish添加flavors 新建dishDto 既有dish又有flavors

            dishDtoList = list.stream().map((item) -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item, dishDto);
            //拿到flavors 根据dishId去dish_flavors表查
/*            LambdaQueryWrapper<DishFlavor> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(DishFlavor::getDishId, item.getId());
            List<DishFlavor> dishFlavorList = dishFlavorService.list(queryWrapper1); //拿到当前菜品的口味
            dishDto.setFlavors(dishFlavorList);//写入dishDto*/
                dishDto = dishService.getByIdWithFlavor(item.getId());
                return dishDto;
            }).toList();
            redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
            return Result.success(dishDtoList);
        }
    }
    @PostMapping("/status/0")
    public Result<String> disable(@RequestParam List<Long> ids){
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, 0);
        dishService.update(updateWrapper);
        changeStatusWithRedis(updateWrapper);
        log.info("停售菜品");
        return Result.success("菜品已停售");
    }
/*    @PostMapping("/status/0")
    public Result<String> disable(@RequestParam List<Long> ids){
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        Dish dish = new Dish();
        dish.setStatus(0);
        dishService.update(dish, queryWrapper);
        //根据传来的菜品id 拿到种类id 根据种类清除缓存 这样状态改变也会更新到缓存
        List<Dish> list = dishService.list(queryWrapper);
        List<Long> categoryIds=list.stream().map(Dish::getCategoryId).toList();
        for(long id : categoryIds){
            String key = "dish_" + id + "_1";
            redisTemplate.delete(key);
        }
        log.info("停售菜品");
        return Result.success("菜品已停售");
    }*/
    @PostMapping("/status/1")
    public Result<String> enable(@RequestParam List<Long> ids){
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, 1);
        dishService.update(updateWrapper);
        changeStatusWithRedis(updateWrapper);
        log.info("启售菜品");
        return Result.success("菜品已启售");
    }

    public void changeStatusWithRedis(LambdaUpdateWrapper<Dish> updateWrapper){
        List<Dish> list = dishService.list(updateWrapper);
        List<Long> categoryIds=list.stream().map(Dish::getCategoryId).toList();
        for(long id : categoryIds){
            String key = "dish_" + id + "_1";
            redisTemplate.delete(key);
        }
    }

}
