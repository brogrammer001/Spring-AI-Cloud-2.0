<template>
  <div class="login">
    <!-- 背景装饰光斑 -->
    <div class="bg-blobs">
      <span class="blob blob-1"></span>
      <span class="blob blob-2"></span>
      <span class="blob blob-3"></span>
    </div>

    <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="login-brand">
        <div class="brand-logo"><i class="fas fa-bolt"></i></div>
        <h3 class="title">{{ title }}</h3>
        <p class="subtitle">欢迎回来，请登录您的账户</p>
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

const title = import.meta.env.VITE_APP_TITLE
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
.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  overflow: hidden;
  background:
    radial-gradient(at 20% 20%, rgba(99, 102, 241, 0.55) 0px, transparent 50%),
    radial-gradient(at 80% 10%, rgba(56, 189, 248, 0.50) 0px, transparent 50%),
    radial-gradient(at 70% 80%, rgba(168, 85, 247, 0.45) 0px, transparent 50%),
    radial-gradient(at 10% 90%, rgba(59, 130, 246, 0.45) 0px, transparent 50%),
    linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%);
}

/* 背景浮动光斑 */
.bg-blobs {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;

  .blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(60px);
    opacity: 0.6;
    animation: float 18s ease-in-out infinite;
  }

  .blob-1 {
    width: 420px;
    height: 420px;
    background: radial-gradient(circle, #6366f1, transparent 70%);
    top: -120px;
    left: -80px;
    animation-delay: 0s;
  }

  .blob-2 {
    width: 480px;
    height: 480px;
    background: radial-gradient(circle, #06b6d4, transparent 70%);
    bottom: -160px;
    right: -120px;
    animation-delay: -6s;
  }

  .blob-3 {
    width: 360px;
    height: 360px;
    background: radial-gradient(circle, #a855f7, transparent 70%);
    top: 40%;
    left: 55%;
    animation-delay: -12s;
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.08); }
  66% { transform: translate(-30px, 40px) scale(0.95); }
}

/* 品牌区 */
.login-brand {
  text-align: center;
  margin-bottom: 28px;

  .brand-logo {
    width: 64px;
    height: 64px;
    margin: 0 auto 16px;
    border-radius: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    background: linear-gradient(135deg, #6366f1 0%, #3b82f6 50%, #06b6d4 100%);
    box-shadow: 0 12px 32px rgba(99, 102, 241, 0.45);
  }
}

.title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #ffffff 0%, #c7d2fe 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 0.3px;
}

/* 毛玻璃登录卡 */
.login-form {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: calc(100vw - 32px);
  padding: 40px 36px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.35);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  backdrop-filter: blur(24px) saturate(180%);
  animation: cardIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;

  :deep(.el-input) {
    height: 44px;

    .el-input__wrapper {
      background: rgba(255, 255, 255, 0.9);
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.1);
      transition: all 0.25s ease;
    }

    .el-input__wrapper.is-focus {
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.35), 0 2px 8px rgba(15, 23, 42, 0.1);
    }

    input {
      height: 44px;
      color: #1e293b;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
    color: #64748b;
  }
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 24px;

  :deep(.el-checkbox__label) {
    color: rgba(255, 255, 255, 0.75);
    font-size: 13px;
  }

  .link-type {
    color: #c7d2fe;
    font-size: 13px;

    &:hover {
      color: #fff;
    }
  }
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 46px !important;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none !important;
  background: linear-gradient(135deg, #6366f1 0%, #3b82f6 50%, #06b6d4 100%) !important;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(99, 102, 241, 0.55);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 10px;
  }
}

.el-login-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: 12px;
  letter-spacing: 1px;
  z-index: 1;
}

.login-code-img {
  height: 44px;
  padding-left: 12px;
  border-radius: 10px;
}
</style>
