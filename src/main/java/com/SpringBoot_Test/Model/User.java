package com.SpringBoot_Test.Model; // 注意包名，不是 Controller！

public class User {

    // 只有字段 + get + set
    private Long id;
    private String name;
    private Integer age;

    // getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    // setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}