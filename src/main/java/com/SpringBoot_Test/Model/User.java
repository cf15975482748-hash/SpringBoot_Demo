package com.SpringBoot_Test.Model; // 注意包名，不是 Controller！

public class User {

    // 只有字段 + get + set
    private Long id;
    private String name;
    private Float score;

    // getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Float getScore() {
        return score;
    }

    // setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}