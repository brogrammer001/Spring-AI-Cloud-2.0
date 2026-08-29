<template>
  <div class="sidebar-logo-container" :class="{ 'collapse': collapse }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else class="sidebar-title">{{ title }}</h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 class="sidebar-title">{{ title }}</h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import logo from '@/assets/logo/logo.png'
import useSettingsStore from '@/store/modules/settings'
import variables from '@/assets/styles/variables.module.scss'

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

const title = import.meta.env.VITE_APP_TITLE
const settingsStore = useSettingsStore()
const sideTheme = computed(() => settingsStore.sideTheme)

// 获取Logo背景色（浅色主题与导航栏同色，连成顶部白色横带）
const getLogoBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-bg)'
  }
  return sideTheme.value === 'theme-dark' ? variables.menuBg : 'var(--navbar-bg)'
})

// 获取Logo文字颜色
const getLogoTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-text)'
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightText
  }
  return sideTheme.value === 'theme-dark' ? '#fff' : variables.menuLightText
})
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 64px;
  /* 用 flex 替代 line-height: 64px，避免与子项 line-height 冲突导致文字被挤 */
  background: v-bind(getLogoBackground);
  overflow: hidden;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 0 16px;
    transition: opacity 0.2s ease;
    /* 覆盖 sidebar.scss 中 .sidebar-container a 的 overflow: hidden，
     * 由内部 .sidebar-title 自行处理溢出，避免图片和标题整体被裁剪 */
    overflow: visible;

    &:hover {
      opacity: 0.85;
    }

    & .sidebar-logo {
      /* 参考图：品牌渐变图标 + 圆角胶囊背景 */
      width: 32px;
      height: 32px;
      border-radius: var(--radius-sm, 8px);
      /* 极淡玫红光环，强化品牌识别 */
      box-shadow: 0 2px 8px rgba(240, 67, 110, 0.18);
      object-fit: cover;
      /* flex 布局下防止图标被压缩 */
      flex-shrink: 0;
    }

    & .sidebar-title {
      margin: 0;
      color: v-bind(getLogoTextColor);
      /* 参考 design.md 4.7：品牌名 17~18px 700 */
      font-weight: 700;
      line-height: 1.2;
      font-size: 17px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
      letter-spacing: -0.2px;
      /* 不占满 flex 剩余空间，让「图标 + 标题」整体由 justify-content: center 居中 */
      flex: 0 1 auto;
      min-width: 0;
      max-width: 100%;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  &.collapse {
    .sidebar-logo-link {
      padding: 0;
      /* 折叠态只显示图标，强制居中 */
      justify-content: center;
    }
  }
}
</style>