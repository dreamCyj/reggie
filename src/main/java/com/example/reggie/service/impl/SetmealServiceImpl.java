package com.example.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.reggie.common.CustomException;
import com.example.reggie.common.Result;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.DishFlavor;
import com.example.reggie.entity.Setmeal;
import com.example.reggie.entity.SetmealDish;
import com.example.reggie.mapper.SetmealMapper;
import com.example.reggie.service.SetmealDishService;
import com.example.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    @Transactional
    public void saveWithDishes(SetmealDto setmealDto) {
        //保存套餐基本信息到setmeal
        this.save(setmealDto);//插入setmeal表后 就有了Id
        Long setmealId = setmealDto.getId();//拿到套餐id
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes(); //拿到套餐里的菜品
        setmealDishes = setmealDishes.stream().map((item) -> { //为每个菜品数据加上套餐id，绑定在一起
            item.setSetmealId(setmealId);
            return item;
        }).toList();
        //保存菜品到setmeal_dish
        setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    @Transactional
    public void deleteWithDishes(List<Long> ids) {
        //查询套餐状态是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus,"1");
        if(this.count(queryWrapper) > 0){
            throw new CustomException("套餐正在售卖中，无法删除");
        }
        //删除setmeal表中的
        this.removeByIds(ids);
        //删除setmeal_dish中的关联信息
        LambdaQueryWrapper<SetmealDish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(SetmealDish::getSetmealId, ids); //根据套餐ids在setmeal_dish中找到关联信息
        setmealDishService.remove(queryWrapper1);
    }

    @Override
    public SetmealDto getByIdWithDishes(Long id) {//传过来的是套餐id
        //从setmeal表中
        Setmeal setmeal = this.getById(id);
        //从setmeal_dish表中
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    @Override
    public void updateWithDishes(SetmealDto setmealDto) {
        //修改setmeal
        this.updateById(setmealDto);
        //修改setmeal_dish  删除旧的 插入新的 是为更新
        //删除旧的
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        //插入新的
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        List<SetmealDish> list = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).toList();

        setmealDishService.saveBatch(list);
    }
}
