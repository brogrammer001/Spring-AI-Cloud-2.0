<template>
  <div class="login">
    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="login-brand">
        <div class="brand-logo"><i class="fas fa-bolt"></i></div>
        <h3 class="title">{{ title }}</h3>
        <p class="subtitle">欢迎回来，请登录您的账户</p>
        <div v-if="mockMode" class="offline-tip">
          <i class="fas fa-plug-circle-xmark"></i>
          离线演示模式：未连接后端接口，任意账号密码即可登录
        </div>
      </div>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
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
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          size="large"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <div class="login-options">
        <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
        <div v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </div>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          class="login-btn"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import { isMockEnabled } from '@/utils/mock'

const title = import.meta.env.VITE_APP_TITLE
const mockMode = isMockEnabled()
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
        if (captchaEnabled.value) {
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
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
/* ===== 淡紫马卡龙风（参考设计：玫瑰红主色 + 薰衣草底） ===== */
$primary: #f0436e;          /* 玫瑰红主色 */
$primary-hover: #d9305c;
$text-strong: #262336;
$text-regular: #4b4861;
$text-muted: #8b8899;
$text-faint: #b6b3c2;
$border: #eceaf4;
$border-hover: #dcd8ea;

.login {
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

  /* 柔和光斑：让毛玻璃卡片有内容可透（Linear 风格） */
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

/* 登录卡：半透明白 + 毛玻璃（Linear / Vercel 质感） */
.login-form {
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
.login-brand {
  text-align: center;
  margin-bottom: 32px;

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

/* 离线演示模式提示条 */
.offline-tip {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  color: #b7791f;
  background: rgba(245, 158, 11, 0.10);
  border: 1px solid rgba(245, 158, 11, 0.25);

  i {
    margin-right: 6px;
  }
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 24px;

  :deep(.el-checkbox__label) {
    color: $text-regular;
    font-size: 13px;
  }

  .link-type {
    color: $primary;
    font-size: 13px;
    transition: color 0.3s ease;

    &:hover {
      color: $primary-hover;
    }
  }
}

/* 登录按钮 */
.login-btn {
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

.login-code {
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

.login-code-img {
  height: 44px;
  padding-left: 12px;
  border-radius: 8px;
}

.el-login-footer {
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
