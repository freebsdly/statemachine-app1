package com.trina.core.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AuthUserVO {
    @Schema(description = "账号")
    private String account;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户id")
    private String userId;

    @Schema(title = "头像")
    private String avatar;

    @Schema(title = "性别(0-默认未知,1-男,2-女)")
    private Integer gender;

    @Schema(description = "工号")
    private String staffNumber;

    @Schema(description = "用户名全称")
    private String realName;

    @Schema(description = "用户昵称")
    private String nickName;

    @Schema(title = "天讯ID")
    private String unionId;

    @Schema(description = "权限标识")
    private List<String> permissions;

    @Schema(description = "角色Code")
    private List<String> roles;

    @Schema(description = "token值")
    private String tokenValue;

    @Schema(description = "统一认证的accessToken")
    private String authAccessToken;
}
