<template>
  <div class="register">
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <div class="register-brand">
        <div class="brand-logo"><i class="fas fa-bolt"></i></div>
        <h3 class="title">{{ title }}</h3>
        <p class="subtitle">创建您的账户，开启全新体验</p>
      </div>
      <el-form-item prop="username">
        <el-input
          v-model="registerForm.username"
          type="text"
          size="large"
          auto-complete="off"
          placeholder="账号"
        >
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="确认密码"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          size="large"
          v-model="registerForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="register-code">
          <img :src="codeUrl" @click="getCode" class="register-code-img"/>
        </div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          class="register-btn"
          @click.prevent="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注 册 中...</span>
        </el-button>
      </el-form-item>
      <div class="register-options">
        <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
      </div>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()

const registerForm = ref({
  username: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入您的账号" },
    { min: 2, max: 20, message: "用户账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang='scss' scoped>
/* ===== 淡紫马卡龙风（与登录页统一：玫瑰红主色 + 薰衣草底 + 毛玻璃卡） ===== */
$primary: #f0436e;
$primary-hover: #d9305c;
$text-strong: #262336;
$text-regular: #4b4861;
$text-muted: #8b8899;
$text-faint: #b6b3c2;
$border: #eceaf4;
$border-hover: #dcd8ea;

.register {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 100vh;
  padding: 24px;
  overflow: hidden;
  background:
    radial-gradient(at 12% 12%, rgba(250, 214, 226, 0.55) 0px, transparent 42%),
    radial-gradient(at 88% 18%, rgba(221, 214, 246, 0.55) 0px, transparent 42%),
    radial-gradient(at 80% 92%, rgba(214, 228, 250, 0.50) 0px, transparent 42%),
    #f4f2f9;

  /* 柔和光斑：让毛玻璃卡片有内容可透 */
  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    filter: blur(90px);
    pointer-events: none;
    z-index: 0;
  }

  &::before {
    width: 480px;
    height: 480px;
    top: -140px;
    left: -100px;
    background: radial-gradient(circle, rgba(240, 67, 110, 0.16) 0%, transparent 70%);
  }

  &::after {
    width: 520px;
    height: 520px;
    bottom: -180px;
    right: -120px;
    background: radial-gradient(circle, rgba(167, 139, 250, 0.20) 0%, transparent 70%);
  }
}

/* 注册卡：半透明白 + 毛玻璃 */
.register-form {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: calc(100vw - 48px);
  padding: 40px;
  background: rgba(255, 255, 255, 0.65);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  box-shadow:
    0 1px 2px rgba(17, 24, 39, 0.04),
    0 12px 32px rgba(17, 24, 39, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
  animation: cardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;

  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 1px 2px rgba(17, 24, 39, 0.04),
      0 20px 44px rgba(17, 24, 39, 0.10),
      inset 0 1px 0 rgba(255, 255, 255, 0.6);
  }

  :deep(.el-input) {
    height: 44px;

    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.7);
      border: 1px solid rgba(229, 231, 235, 0.9);
      border-radius: 8px;
      box-shadow: none;
      transition: all 0.3s ease;
    }

    .el-input__wrapper:hover {
      border-color: $border-hover;
      background: rgba(255, 255, 255, 0.85);
    }

    .el-input__wrapper.is-focus {
      border-color: $primary !important;
      background: rgba(255, 255, 255, 0.95);
      box-shadow: 0 0 0 3px rgba(240, 67, 110, 0.12) !important;
    }

    input {
      height: 44px;
      color: $text-strong;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
    color: $text-faint;
  }
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 品牌区 */
.register-brand {
  text-align: center;
  margin-bottom: 28px;

  .brand-logo {
    width: 44px;
    height: 44px;
    margin: 0 auto 16px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #ffffff;
    background: linear-gradient(135deg, $primary 0%, #ff7a9c 100%);
    box-shadow: 0 4px 12px rgba(240, 67, 110, 0.30);
  }
}

.title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: $text-strong;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: $text-muted;
}

/* 注册按钮 */
.register-btn {
  width: 100%;
  height: 44px !important;
  border-radius: 8px !important;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border: none !important;
  background: $primary !important;
  color: #ffffff !important;
  box-shadow: 0 1px 2px rgba(240, 67, 110, 0.22) !important;
  transition: all 0.3s ease !important;

  &:hover {
    background: $primary-hover !important;
    transform: translateY(-2px) !important;
    box-shadow: 0 8px 20px rgba(240, 67, 110, 0.30) !important;
  }

  &:active {
    transform: translateY(0) !important;
  }
}

.register-options {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;

  .link-type {
    color: $primary;
    font-size: 13px;
    transition: color 0.3s ease;

    &:hover {
      color: $primary-hover;
    }
  }
}

.register-code {
  width: 33%;
  height: 44px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 8px;
    border: 1px solid $border;
  }
}

.register-code-img {
  height: 44px;
  padding-left: 12px;
  border-radius: 8px;
}

.el-register-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: $text-faint;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: 12px;
  letter-spacing: 0.5px;
  z-index: 1;
}
</style>
