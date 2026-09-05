<template>
  <div class="errPage-container">
    <el-button icon="arrow-left" class="pan-back-btn" @click="back">
      返回
    </el-button>
    <el-row class="err-card">
      <el-col :span="12" class="err-text">
        <h1 class="text-jumbo text-ginormous">
          401错误!
        </h1>
        <h2>您没有访问权限！</h2>
        <h6>对不起，您没有访问权限，请不要进行非法操作！您可以返回主页面</h6>
        <ul class="list-unstyled">
          <li class="link-type">
            <router-link to="/">
              回首页
            </router-link>
          </li>
        </ul>
      </el-col>
      <el-col :span="12" class="err-illu">
        <img :src="errGif" width="313" height="428" alt="Girl has dropped her ice cream.">
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import errImage from "@/assets/401_images/401.gif"

let { proxy } = getCurrentInstance()

const errGif = ref(errImage + "?" + +new Date())

function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: "/" })
  } else {
    proxy.$router.go(-1)
  }
}
</script>

<style lang="scss" scoped>
/* ===== 淡紫马卡龙风：悬浮白卡 + 玫红主色 ===== */
.errPage-container {
  width: 820px;
  max-width: calc(100% - 40px);
  margin: 60px auto;

  .pan-back-btn {
    margin-bottom: 20px;
    border-radius: 999px;
    padding: 0 20px;
  }
}

.err-card {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 40px 48px;
  background: var(--card-bg, #ffffff);
  border: 1px solid var(--card-border, #eceaf4);
  border-radius: var(--radius-xl, 20px);
  box-shadow: var(--shadow-lg);
  animation: errIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes errIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.err-text {
  .text-jumbo {
    margin: 0 0 12px;
    font-size: 56px;
    font-weight: 700;
    line-height: 1.1;
    background: linear-gradient(135deg, #f0436e 0%, #a78bfa 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: transparent;
  }

  h2 {
    margin: 0 0 10px;
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary, #262336);
  }

  h6 {
    margin: 0 0 20px;
    font-size: 14px;
    font-weight: 400;
    line-height: 1.7;
    color: var(--text-secondary, #8b8899);
  }
}

.err-illu {
  text-align: center;

  img {
    max-width: 100%;
    height: auto;
  }
}

.list-unstyled {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 14px;

  li {
    padding-bottom: 5px;
  }

  a {
    font-weight: 600;
    color: var(--el-color-primary, #f0436e);
    text-decoration: none;
    transition: color 0.25s ease;

    &:hover {
      color: #d9305c;
      text-decoration: underline;
    }
  }
}
</style>
