package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import com.mall.common.core.xss.Xss;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SysUserBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "userId", column = "user_id", name = "用户ID", excludeForm = true)
    @JsonPropertyDescription("用户ID，选择记录后自动回传，无需手动填写")
    private Long userId;

    @FieldRemark(field = "deptId", column = "dept_id", name = "部门ID", excludeForm = true)
    @JsonPropertyDescription("部门ID，选择部门后自动回传，无需手动填写")
    private Long deptId;

    @FieldRemark(field = "userName", column = "user_name", name = "用户账号")
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    @JsonPropertyDescription("用户账号，例如 zhangsan、lisi")
    private String userName;

    @FieldRemark(field = "nickName", column = "nick_name", name = "用户昵称")
    @Xss(message = "用户昵称不能包含脚本字符")
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    @JsonPropertyDescription("用户昵称，例如 张三、李四")
    private String nickName;

    @FieldRemark(field = "deptName", column = "dept_name", name = "部门名称")
    @JsonPropertyDescription("部门名称，例如 财务部、研发部")
    private String deptName;

    @FieldRemark(field = "phonenumber", column = "phonenumber", name = "手机号码")
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    @JsonPropertyDescription("手机号码，11位数字")
    private String phonenumber;

    @FieldRemark(field = "email", column = "email", name = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    @JsonPropertyDescription("邮箱")
    private String email;

    @FieldRemark(field = "sex", column = "sex", name = "性别", readConverterExp = "0=男,1=女,2=未知")
    @Pattern(regexp = "^[男女]$", message = "性别填写错误，只能填写'男'或'女'")
    @JsonPropertyDescription("性别，只能是男或女")
    private String sex;

    @FieldRemark(field = "status", column = "status", name = "用户状态", readConverterExp = "0=正常,1=停用", excludeForm = true)
    @JsonPropertyDescription("用户状态：有效/启用表示正常，无效/停用表示禁用，仅在update操作时使用")
    private String status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
