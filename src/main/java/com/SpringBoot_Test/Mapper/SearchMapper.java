package com.SpringBoot_Test.Mapper;

import com.SpringBoot_Test.Model.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

// 标记这是 MyBatis 的 Mapper
@Mapper
public interface SearchMapper {

    // 查询当前数据库中的所有表名
    @Select("SHOW TABLES")
    List<String> getAllTables();

    // 检查表是否存在
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = (SELECT DATABASE()) AND table_name = #{tableName}")
    int checkTableExists(@Param("tableName") String tableName);

    // 注解方式查询（支持动态表名）
    @Select("SELECT * FROM ${tableName}")
    List<User> findAll(@Param("tableName") String tableName);

    // 创建新表
    @Update("CREATE TABLE IF NOT EXISTS ${tableName} (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255), " +
            "age INT)")
    void createTable(@Param("tableName") String tableName);

    // 删除表
    @Update("DROP TABLE IF EXISTS ${tableName}")
    void dropTable(@Param("tableName") String tableName);

    // 插入初始数据 (仅 ID 1-60)
    @Insert("<script>" +
            "INSERT INTO ${tableName} (id) VALUES " +
            "<foreach collection='ids' item='id' separator=','>" +
            "(#{id})" +
            "</foreach>" +
            "</script>")
    void insertInitialIds(@Param("tableName") String tableName, @Param("ids") List<Long> ids);
}
