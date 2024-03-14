package com.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reggie.dto.DishDto;
import com.example.reggie.entity.Dish;
import com.example.reggie.entity.DishFlavor;
import com.example.reggie.mapper.DishMapper;
import com.example.reggie.service.DishFlavorService;
import com.example.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    /**
     * 新增菜品到dish，同时保存口味到dish_flavor
     * @param dishDto
     */

    @Autowired
    private DishFlavorService dishFlavorService;
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品到dish
        this.save(dishDto);//插入dish表后 就有了Id
        Long dishId = dishDto.getId();//拿到菜品id
        List<DishFlavor> flavors = dishDto.getFlavors(); //拿到菜品口味
        flavors = flavors.stream().map((item) -> { //为每个口味数据加上菜品id，绑定在一起
            item.setDishId(dishId);
            return item;
        }).toList();
        //保存口味到dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getByIdWithFlavor(Long id) { //想要得到的是dish表中的基本信息加上dish_flavor表中对应的口味信息
        //从dish表查询基本信息
        Dish dish = this.getById(id);
        //从dish_flavor表查询口味信息
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());//从dish_flavor表中查询dish_id为dish.getId()的
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);//将dish的赋给dishDto，这样dishDto中有dish表中的基本信息
        dishDto.setFlavors(flavors);//为dishDto加上dish_flavor表中对应的口味信息 这样dishDto就是我们想要的
        return dishDto;
    }

    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        this.updateById(dishDto);
        //先清除当前口味数据 dish_flavor进行delete
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        /**
         * dishFlavorService.updateBatchById(dishDto.getFlavors());只能改现有口味 不能新增也不能删除
         */
        //再插入提交过来的数据 dish_flavor进行insert 等同于新增
        List<DishFlavor> flavors = dishDto.getFlavors(); //拿到菜品口味
        flavors = flavors.stream().map((item) -> { //为每个口味数据加上菜品id，绑定在一起
            item.setDishId(dishDto.getId());
            return item;
        }).toList();
        //保存口味到dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

}
