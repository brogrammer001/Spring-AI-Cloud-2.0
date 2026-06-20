package com.mall.chatmcp.bo;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.xss.Xss;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public class SysUserBo {

    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    @JsonPropertyDescription("用户账号，例如 zhangsan、lisi")
    private String userName;

    @Xss(message = "用户昵称不能包含脚本字符")
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    @JsonPropertyDescription("用户昵称，例如 张三、李四")
    private String nickName;

    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    @JsonPropertyDescription("部门名称，例如 财务部、研发部")
    private String deptName;

    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    @JsonPropertyDescription("手机号码，11位数字")
    private String phonenumber;

    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    @JsonPropertyDescription("邮箱")
    private String email;

    @Pattern(regexp = "^[男女]$", message = "性别填写错误，只能填写'男'或'女'")
    @JsonPropertyDescription("性别，只能是男或女")
    private String sex;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
