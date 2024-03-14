package com.example.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    public void saveWithDishes(SetmealDto setmealDto);

    void deleteWithDishes(List<Long> ids);

    SetmealDto getByIdWithDishes(Long id);

    void updateWithDishes(SetmealDto setmealDto);
}
