<template>
  <div class="navbar" :class="'nav' + settingsStore.navType">
    <hamburger id="hamburger-container" :is-active="appStore.sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb v-if="settingsStore.navType == 1" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="settingsStore.navType == 2" id="topmenu-container" class="topmenu-container" />
    <template v-if="settingsStore.navType == 3">
      <logo v-show="settingsStore.sidebarLogo" :collapse="false"></logo>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>

    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <header-search id="header-search" class="right-menu-item hover-effect" />

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip content="主题模式" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect " @click="toggleTheme">
            <svg-icon v-if="settingsStore.isDark" icon-class="sunny" />
            <svg-icon v-if="!settingsStore.isDark" icon-class="moon" />
          </div>
        </el-tooltip>

        <el-tooltip content="消息通知" effect="dark" placement="bottom">
          <header-notice id="header-notice" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>

      <el-dropdown @command="handleCommand" class="avatar-container" trigger="hover">
        <div class="avatar-wrapper">
          <img :src="userStore.avatar" class="user-avatar" />
          <span class="user-nickname"> {{ userStore.nickName }} </span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item command="setLayout" v-if="settingsStore.showSettings">
                <span>布局设置</span>
            </el-dropdown-item>
            <el-dropdown-item command="lockScreen">
                <span>锁定屏幕</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import {ElMessageBox} from 'element-plus'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import HeaderSearch from '@/components/HeaderSearch'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import HeaderNotice from './HeaderNotice'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const lockStore = useLockStore()
const settingsStore = useSettingsStore()

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command) {
  switch (command) {
    case "setLayout":
      setLayout()
      break
    case "lockScreen":
      lockScreen()
      break
    case "logout":
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/index'
    })
  }).catch(() => { })
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout')
}

function lockScreen() {
  const currentPath = route.fullPath
  lockStore.lockScreen(currentPath)
  router.push('/lock')
}

async function toggleTheme(event) {
  const x = event?.clientX || window.innerWidth / 2
  const y = event?.clientY || window.innerHeight / 2
  const wasDark = settingsStore.isDark

  const isReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
  const isSupported = document.startViewTransition && !isReducedMotion

  if (!isSupported) {
    settingsStore.toggleTheme()
    return
  }

  try {
    const transition = document.startViewTransition(async () => {
      await new Promise((resolve) => setTimeout(resolve, 10))
      settingsStore.toggleTheme()
      await nextTick()
    })
    await transition.ready

    const endRadius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
    const clipPath = [`circle(0px at ${x}px ${y}px)`, `circle(${endRadius}px at ${x}px ${y}px)`]
    document.documentElement.animate(
      {
        clipPath: !wasDark ? [...clipPath].reverse() : clipPath
      }, {
        duration: 650,
        easing: "cubic-bezier(0.4, 0, 0.2, 1)",
        fill: "forwards",
        pseudoElement: !wasDark ? "::view-transition-old(root)" : "::view-transition-new(root)"
      }
    )
    await transition.finished
  } catch (error) {
    console.warn("View transition failed, falling back to immediate toggle:", error)
    settingsStore.toggleTheme()
  }
}
</script>

<style lang='scss' scoped>
.navbar.nav3 {
  .hamburger-container {
    display: none !important;
  }
}

.navbar {
  height: 64px;
  overflow: hidden;
  position: relative;
  /* 参考图：顶部白色横带，与侧边栏 logo 区同色连成一体 */
  background: var(--navbar-bg);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  backdrop-filter: blur(12px) saturate(160%);
  box-shadow: none;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  padding: 0 20px;

  /* 当未启用 TagsView 时，由 navbar 承担顶部横带底部的 1px 极淡分界线
   * 启用 TagsView 时由 TagsView 自身提供分界，navbar 不重复
   * 注：实际生效的样式见文件末尾非 scoped 块（避免 scoped 属性作用域问题）
   */

  .hamburger-container {
    line-height: 64px;
    height: 100%;
    cursor: pointer;
    transition: all 0.25s ease;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-right: 8px;
    padding: 0 10px;
    border-radius: var(--radius-full);
    color: var(--el-color-primary, #f0436e);

    &:hover {
      background: rgba(240, 67, 110, 0.08);
      color: var(--el-color-primary, #f0436e);
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;
  }

  .topmenu-container {
    position: absolute;
    left: 56px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .right-menu {
    height: 100%;
    line-height: 64px;
    display: flex;
    align-items: center;
    /* design.md 4.5 间距刻度：图标之间 12px，避免挤在一起 */
    gap: 12px;
    margin-left: auto;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 36px;
      padding: 0 6px;
      font-size: 17px;
      color: var(--el-color-primary, #f0436e);
      vertical-align: text-bottom;

      &.hover-effect {
        width: 36px;
        padding: 0;
        cursor: pointer;
        transition: all 0.25s ease;
        border-radius: var(--radius-full);

        &:hover {
          /* design.md 4.5 右侧图标 hover 玫红轻染 + 文字玫红 */
          background: rgba(240, 67, 110, 0.10);
          color: var(--el-color-primary, #f0436e);
        }
      }

      &.theme-switch-wrapper {
        display: flex;
        align-items: center;

        svg {
          transition: transform 0.3s;

          &:hover {
            transform: scale(1.15);
          }
        }
      }
    }

    .avatar-container {
      width: auto;
      height: auto;
      margin-left: 4px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 5px 12px 5px 5px;
        height: auto;
        cursor: pointer;
        border-radius: var(--radius-full);
        transition: all 0.25s ease;

        &:hover {
          /* design.md 4.5 头像区 hover 胶囊轻染 */
          background: rgba(240, 67, 110, 0.05);
        }

        .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          object-fit: cover;
          /* 参考图：白边圆环头像 */
          box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px rgba(240, 67, 110, 0.25);
          transition: box-shadow 0.25s ease;
        }

        &:hover .user-avatar {
          /* hover 时玫红光环加深 */
          box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px rgba(240, 67, 110, 0.45);
        }

        .user-nickname {
          font-size: 14px;
          font-weight: 600;
          color: var(--text-primary, #262336);
        }

        i {
          cursor: pointer;
          font-size: 12px;
          color: var(--text-secondary, #8b8899);
        }
      }
    }
  }
}
</style>

<style lang="scss">
/* 当未启用 TagsView 时（main-container 无 hasTagsView 类），
 * 由 navbar 承担顶部横带底部的 1px 极淡分界线
 * 启用 TagsView 时由 TagsView 自身提供分界，navbar 不重复
 * design.md 4.5：横带底部 1px 极淡分界 rgba(38,35,54,0.05)
 */
.main-container:not(.hasTagsView) .navbar {
  border-bottom: 1px solid rgba(38, 35, 54, 0.05);
}
</style>
