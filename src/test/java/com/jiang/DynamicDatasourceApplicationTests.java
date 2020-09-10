package com.jiang;

import com.jiang.context.DynamicDataSourceContextHolder;
import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;

@SpringBootTest
class DynamicDatasourceApplicationTests {

    @Autowired
    UserDao userDao;

    @Test
    void contextLoads() {
        slave();
    }

    void slave(){
        DynamicDataSourceContextHolder.setContextKey(SLAVE1);
        UserDTO userDTO = new UserDTO("ceshi", 20);
        Assert.isTrue(userDao.insertUser(userDTO) > 0, "insert sucess");
    }

}
