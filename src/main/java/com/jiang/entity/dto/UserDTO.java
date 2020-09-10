package com.jiang.entity.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 22:46
 */
@Setter
@Getter
@NoArgsConstructor
public class UserDTO {

    private int id;

    private String name;

    private Integer age;

    public UserDTO(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}