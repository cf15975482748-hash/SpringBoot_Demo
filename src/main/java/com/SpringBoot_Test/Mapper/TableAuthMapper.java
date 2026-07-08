package com.SpringBoot_Test.Mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TableAuthMapper {

    // 更新业务表允许查看的学生白名单
    @Update("ALTER TABLE `${tableName}` MODIFY COLUMN allow_student TEXT")
    void ensureAllowStudentColumn(@Param("tableName") String tableName);

    // 这个实际上是在 SearchMapper 里的 createTable 修改，这里提供更新白名单的方法
    @Update("UPDATE information_schema.tables SET table_comment = #{allowStudent} WHERE table_schema = (SELECT DATABASE()) AND table_name = #{tableName}")
    void updateAllowStudentComment(@Param("tableName") String tableName, @Param("allowStudent") String allowStudent);
    
    @Update("UPDATE `${tableName}` SET allow_student = #{allowStudent}")
    void updateTableWhitelist(@Param("tableName") String tableName, @Param("allowStudent") String allowStudent);

    // 根据登录老师账号，查询所有自己创建的业务表
    @Select("SELECT table_name FROM information_schema.columns " +
            "WHERE table_schema = (SELECT DATABASE()) " +
            "AND column_name = 'create_user' " +
            "AND table_name NOT IN ('admin', 'teacher', 'student')")
    List<String> getAllBusinessTables();

    // 获取表的创建者和白名单（取第一行即可，因为全表一致）
    @Select("SELECT create_user, allow_student FROM `${tableName}` LIMIT 1")
    Map<String, Object> getTableAuthInfo(@Param("tableName") String tableName);
}
