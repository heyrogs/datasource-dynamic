package com.jiang.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-10 21:59
 */
@AllArgsConstructor
@Getter
public enum DBConstant {

    DB_KEY_MASTER("masterDataSource", "主数据库，也是默认使用的数据库"),
    DB_KEY_SLAVE1("slaveDataSource01", "从数据库")
    ;

    private String name;
    private String description;

}
