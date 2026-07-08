package com.SpringBoot_Test.Mapper;

import com.SpringBoot_Test.Model.Admin;
import com.SpringBoot_Test.Model.Teacher;
import com.SpringBoot_Test.Model.Student;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AccountMapper {

    // --- Admin Operations ---
    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin findAdminByUsername(@Param("username") String username);

    @Select("SELECT * FROM admin")
    List<Admin> findAllAdmins();

    @Insert("INSERT INTO admin(username, password, real_name) VALUES(#{username}, #{password}, #{realName})")
    void addAdmin(Admin admin);

    @Update("UPDATE admin SET password = #{password}, real_name = #{realName} WHERE username = #{username}")
    void updateAdmin(Admin admin);

    @Delete("DELETE FROM admin WHERE username = #{username}")
    void deleteAdmin(@Param("username") String username);

    // --- Teacher Operations ---
    @Select("SELECT * FROM teacher WHERE username = #{username}")
    Teacher findTeacherByUsername(@Param("username") String username);

    @Select("SELECT * FROM teacher")
    List<Teacher> findAllTeachers();

    @Insert("INSERT INTO teacher(username, password, real_name) VALUES(#{username}, #{password}, #{realName})")
    void addTeacher(Teacher teacher);

    @Update("UPDATE teacher SET password = #{password}, real_name = #{realName} WHERE username = #{username}")
    void updateTeacher(Teacher teacher);

    @Delete("DELETE FROM teacher WHERE username = #{username}")
    void deleteTeacher(@Param("username") String username);

    // --- Student Operations ---
    @Select("SELECT * FROM student WHERE username = #{username}")
    Student findStudentByUsername(@Param("username") String username);

    @Select("SELECT * FROM student")
    List<Student> findAllStudents();

    @Insert("INSERT INTO student(username, password, real_name) VALUES(#{username}, #{password}, #{realName})")
    void addStudent(Student student);

    @Update("UPDATE student SET password = #{password}, real_name = #{realName} WHERE username = #{username}")
    void updateStudent(Student student);

    @Delete("DELETE FROM student WHERE username = #{username}")
    void deleteStudent(@Param("username") String username);
}
