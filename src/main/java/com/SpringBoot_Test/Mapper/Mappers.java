package com.SpringBoot_Test.Mapper;

import com.SpringBoot_Test.Model.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface Mappers {

    // 修改数据（按 ID 修改，不能修改 ID 本身）
    @Update("UPDATE `${tableName}` SET name = #{user.name}, age = #{user.age} WHERE id = #{user.id}")
    void update(@Param("tableName") String tableName, @Param("user") User user);

    // 删除数据
    @Delete("DELETE FROM `${tableName}` WHERE id = #{id}")
    void delete(@Param("tableName") String tableName, @Param("id") Long id);

    // 模糊查询名字
    @Select("SELECT * FROM `${tableName}` WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<User> searchByName(@Param("tableName") String tableName, @Param("name") String name);
}
