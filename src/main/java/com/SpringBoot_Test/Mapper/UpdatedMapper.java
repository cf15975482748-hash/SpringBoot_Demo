package com.SpringBoot_Test.Mapper;

import com.SpringBoot_Test.Model.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UpdatedMapper {

    // 增加数据（不能更改 ID，由数据库自增或初始插入）
    @Insert("INSERT INTO ${tableName} (name, age) VALUES (#{user.name}, #{user.age})")
    void insert(@Param("tableName") String tableName, @Param("user") User user);

    // 修改数据（按 ID 修改，不能修改 ID 本身）
    @Update("UPDATE ${tableName} SET name = #{user.name}, age = #{user.age} WHERE id = #{user.id}")
    void update(@Param("tableName") String tableName, @Param("user") User user);

    // 删除数据
    @Delete("DELETE FROM ${tableName} WHERE id = #{id}")
    void delete(@Param("tableName") String tableName, @Param("id") Long id);
}
