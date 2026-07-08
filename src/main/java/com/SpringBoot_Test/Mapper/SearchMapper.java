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
    @Select("SELECT * FROM `${tableName}`")
    List<User> findAll(@Param("tableName") String tableName);

    // 检查 ID 是否存在
    @Select("SELECT COUNT(*) FROM `${tableName}` WHERE id = #{id}")
    int countById(@Param("tableName") String tableName, @Param("id") Long id);

    // 创建新表 (新增创建人和白名单字段)
    @Update("CREATE TABLE IF NOT EXISTS `${tableName}` (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(255), " +
            "score FLOAT DEFAULT 0.0, " +
            "create_user VARCHAR(100), " +
            "allow_student TEXT)")
    void createTable(@Param("tableName") String tableName);

    // 删除表 (逻辑在 Controller 或此处增强)
    @Update("DROP TABLE IF EXISTS `${tableName}`")
    void dropTable(@Param("tableName") String tableName);

    // 插入初始数据 (ID 1-60, 包含创建人信息)
    @Insert("<script>" +
            "INSERT INTO `${tableName}` (id, create_user) VALUES " +
            "<foreach collection='ids' item='id' separator=','>" +
            "(#{id}, #{createUser})" +
            "</foreach>" +
            "</script>")
    void insertInitialIds(@Param("tableName") String tableName, @Param("ids") List<Long> ids, @Param("createUser") String createUser);

    // 检查表中是否存在特定列
    @Select("SELECT COUNT(*) FROM information_schema.columns " +
            "WHERE table_schema = (SELECT DATABASE()) " +
            "AND table_name = #{tableName} " +
            "AND column_name = #{columnName}")
    int checkColumnExists(@Param("tableName") String tableName, @Param("columnName") String columnName);

    // 为现有表动态添加字段
    @Update("ALTER TABLE `${tableName}` ADD COLUMN `${columnName}` ${columnType}")
    void addColumnToTable(@Param("tableName") String tableName, @Param("columnName") String columnName, @Param("columnType") String columnType);

    // 初始化权限表
    @Update("CREATE TABLE IF NOT EXISTS admin (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(100) UNIQUE, password VARCHAR(100), real_name VARCHAR(100))")
    void createAdminTable();

    @Update("CREATE TABLE IF NOT EXISTS teacher (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(100) UNIQUE, password VARCHAR(100), real_name VARCHAR(100))")
    void createTeacherTable();

    @Update("CREATE TABLE IF NOT EXISTS student (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(100) UNIQUE, password VARCHAR(100), real_name VARCHAR(100))")
    void createStudentTable();

    @Insert("INSERT IGNORE INTO admin (username, password, real_name) VALUES ('admin', '123456', '超级管理员')")
    void initDefaultAdmin();
}
