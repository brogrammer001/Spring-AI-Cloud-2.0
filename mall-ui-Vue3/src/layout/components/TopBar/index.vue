<template>
  <el-menu class="topbar-menu" :ellipsis="false" :default-active="activeMenu" :active-text-color="theme" mode="horizontal">
    <sidebar-item :key="route.path + index" v-for="(route, index) in topMenus" :item="route" :base-path="route.path" />

    <el-sub-menu index="more" class="el-sub-menu__hide-arrow" v-if="moreRoutes.length > 0">
      <template #title>
        <span>更多菜单</span>
      </template>
      <sidebar-item :key="route.path + index" v-for="(route, index) in moreRoutes" :item="route" :base-path="route.path" />
    </el-sub-menu>
  </el-menu>
</template>

<script setup>
import SidebarItem from '../Sidebar/SidebarItem'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters)
const theme = computed(() => settingsStore.theme)
const device = computed(() => appStore.device)
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

const visibleNumber = ref(5)
const topMenus = computed(() => {
  return permissionStore.sidebarRouters.filter((f) => !f.hidden).slice(0, visibleNumber.value)
})
const moreRoutes = computed(() => {
  return permissionStore.sidebarRouters.filter((f) => !f.hidden).slice(visibleNumber.value)
})
function setVisibleNumber() {
  const width = document.body.getBoundingClientRect().width / 3
  visibleNumber.value = Math.max(1, parseInt(width / 85))
}

onMounted(() => {
  window.addEventListener('resize', setVisibleNumber)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', setVisibleNumber)
})

onMounted(() => {
  setVisibleNumber()
})
</script>

<style lang="scss">
/* TopBar：navType=3 时的顶部菜单，与 TopNav 视觉一致
 * 参考 design.md 4.5：高 64px、菜单项圆角胶囊、玫红激活 + 玫红浅底
 */
.topbar-menu.el-menu--horizontal {
  border-bottom: none !important;

  .el-submenu__title,
  .el-menu-item {
    padding: 0 12px !important;
  }

  > .el-menu-item,
  > .el-sub-menu .el-sub-menu__title {
    height: 64px !important;
    line-height: 64px !important;
    color: var(--text-regular, #4b4861) !important;
    padding: 0 12px !important;
    margin: 0 4px !important;
    border-bottom: none !important;
    border-radius: var(--radius-full, 999px);
    transition: background 0.25s ease, color 0.25s ease;

    &:not(.is-disabled):hover,
    &:not(.is-disabled):focus {
      background-color: rgba(240, 67, 110, 0.06) !important;
      color: var(--el-color-primary, #f0436e) !important;
    }
  }

  > .el-menu-item.is-active,
  > .el-sub-menu.is-active > .el-sub-menu__title {
    color: var(--el-color-primary, #f0436e) !important;
    font-weight: 600;
    background-color: rgba(240, 67, 110, 0.10);
    border-bottom: none !important;
  }
}

/* 激活态 svg 与文字玫红 */
.el-sub-menu.is-active .svg-icon,
.el-menu-item.is-active .svg-icon + span,
.el-sub-menu.is-active .svg-icon + span,
.el-sub-menu.is-active .el-sub-menu__title span {
  color: v-bind(theme);
}

/* topbar more arrow */
.topbar-menu .el-sub-menu .el-sub-menu__icon-arrow {
  position: static;
  margin-left: 6px;
  margin-top: 0px;
  display: block !important;
  color: var(--text-secondary, #8b8899);
}

/* 暗色模式覆盖 */
html.dark .topbar-menu.el-menu--horizontal {
  > .el-menu-item,
  > .el-sub-menu .el-sub-menu__title {
    color: var(--el-text-color-regular) !important;

    &:not(.is-disabled):hover,
    &:not(.is-disabled):focus {
      background-color: rgba(240, 67, 110, 0.18) !important;
    }
  }

  > .el-menu-item.is-active,
  > .el-sub-menu.is-active > .el-sub-menu__title {
    background-color: rgba(240, 67, 110, 0.22);
  }
}
</style>
