package com.cyl.studymate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 普通用户可修改的信息。
 *
 * 不包含账号、密码、角色和状态等敏感字段，避免直接绑定 User 实体造成越权更新。
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String avatarUrl;

    private Integer gender;

    private String phone;

    private String email;

    private String tags;

    private String profile;
}
