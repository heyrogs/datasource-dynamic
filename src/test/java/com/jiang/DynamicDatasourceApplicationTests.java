package com.jiang;

import com.jiang.context.DynamicDataSourceContextHolder;
import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import com.jiang.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.util.List;

import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;

@Slf4j
@SpringBootTest
class DynamicDatasourceApplicationTests {

    @Autowired
    UserDao userDao;

    @Autowired
    UserServiceImpl userService;

    @Test
    void contextLoads() {
        List<UserDTO> userList = userService.getListByMaster();
        log.info("master: {}", userList);
        Assert.notEmpty(userList, "users is empty !");
        userList = userService.getLlistBySlave();
        log.info("slave: {}", userList);
        Assert.notEmpty(userList, "user is empty");
    }

    @Test
    void slave(){
        DynamicDataSourceContextHolder.setContextKey(SLAVE1);
        UserDTO userDTO = new UserDTO("ceshi", 20);
        Assert.isTrue(userDao.insertUser(userDTO) > 0, "insert sucess");
    }

}
