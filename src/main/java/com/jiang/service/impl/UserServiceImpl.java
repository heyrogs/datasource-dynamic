package com.jiang.service.impl;

import com.jiang.annotation.DBSelected;
import com.jiang.dao.UserDao;
import com.jiang.entity.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import static com.jiang.constant.DynamicDataSourceConstant.SLAVE1;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 23:44
 */
@Service
public class UserServiceImpl {

    @Autowired
    private UserDao userDao;

    /**
     *
     * 通过 master 数据库获取数据
     *
     * @return
     */
    @DBSelected
    public List<UserDTO> getListByMaster(){
        return userDao.findList();
    }

    /**
     *
     * 通过 slave 数据库获取数据
     *
     * @return
     */
    @DBSelected(SLAVE1)
    public List<UserDTO> getLlistBySlave(){
        return userDao.findList();
    }

}