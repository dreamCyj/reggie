package com.example.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.reggie.entity.Emp;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmpMapper extends BaseMapper<Emp> {  //BaseMapper<Emp>-->对表emp执行sql操作 BaseMapper<Employee>-->对表employee执行操作

}
