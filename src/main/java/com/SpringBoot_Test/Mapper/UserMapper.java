package com.SpringBoot_Test.Mapper;

import com.SpringBoot_Test.Model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

// 标记这是 MyBatis 的 Mapper
@Mapper
public interface UserMapper {

    // 注解方式查询（最简单）
    @Select("SELECT * FROM user")
    List<User> findAll();
}