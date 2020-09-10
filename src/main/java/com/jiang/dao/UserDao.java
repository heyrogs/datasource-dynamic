package com.jiang.dao;

import com.jiang.entity.dto.UserDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 22:48
 */
@Repository
public interface UserDao {

    @Insert("insert into user(name, age) value (#{name}, #{age})")
    Integer insertUser(UserDTO user);

    @Select("select * from user")
    List<UserDTO> findList();
}
