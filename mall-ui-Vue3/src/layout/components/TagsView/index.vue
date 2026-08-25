<template>
  <div id="tags-view-container" class="tags-view-container">
    <!-- 左切换箭头 -->
    <span class="tags-nav-btn tags-nav-btn--left" :class="{ disabled: !canScrollLeft }" @click="scrollLeft">
      <el-icon><arrow-left /></el-icon>
    </span>

    <!-- 标签滚动区 -->
    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll" @update-arrows="updateArrowState">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :data-path="tag.path"
        :class="{ 'active': isActive(tag), 'has-icon': tagsIcon }"
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
        class="tags-view-item"
        :style="activeStyle(tag)"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        <svg-icon v-if="tagsIcon && tag.meta && tag.meta.icon && tag.meta.icon !== '#'" :icon-class="tag.meta.icon" />
        <span class="tags-view-title">{{ tag.title }}</span>
        <span v-if="!isAffix(tag)" class="tags-close-btn" @click.prevent.stop="closeSelectedTag(tag)">
          <close class="el-icon-close" />
        </span>
      </router-link>
    </scroll-pane>

    <!-- 右切换箭头 -->
    <span class="tags-nav-btn tags-nav-btn--right" :class="{ disabled: !canScrollRight }" @click="scrollRight">
      <el-icon><arrow-right /></el-icon>
    </span>

    <!-- 下拉操作菜单 -->
    <el-dropdown class="tags-action-dropdown" trigger="click" placement="bottom-end" @command="handleDropdownCommand">
      <span class="tags-action-btn">
        <el-icon><arrow-down /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu class="tags-dropdown-menu">
          <el-dropdown-item v-if="!isAffix(selectedDropdownTag)" command="close"><close style="width: 1em; height: 1em;" />关闭当前</el-dropdown-item>
          <el-dropdown-item command="closeOthers"><circle-close style="width: 1em; height: 1em;" />关闭其他</el-dropdown-item>
          <el-dropdown-item command="closeLeft" :disabled="isFirstView()"><back style="width: 1em; height: 1em;" />关闭左侧</el-dropdown-item>
          <el-dropdown-item command="closeRight" :disabled="isLastView()"><right style="width: 1em; height: 1em;" />关闭右侧</el-dropdown-item>
          <el-dropdown-item command="closeAll"><circle-close style="width: 1em; height: 1em;" />全部关闭</el-dropdown-item>
          <el-dropdown-item command="fullscreen" divided>
            <template v-if="!isFullscreen"><full-screen style="width: 1em; height: 1em;" />全屏显示</template>
            <template v-else><close style="width: 1em; height: 1em;" />退出全屏</template>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 刷新按钮 -->
    <span class="tags-action-btn tags-refresh-btn" title="刷新页面" @click="refreshSelectedTag(selectedDropdownTag)">
      <el-icon><refresh-right/></el-icon> 刷新
    </span>

    <!-- 右键上下文菜单 -->
    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)"><refresh-right style="width: 1em; height: 1em;" />刷新页面</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)"><close style="width: 1em; height: 1em;" />关闭当前</li>
      <li @click="closeOthersTags"><circle-close style="width: 1em; height: 1em;" />关闭其他</li>
      <li v-if="!isFirstView()" @click="closeLeftTags"><back style="width: 1em; height: 1em;" />关闭左侧</li>
      <li v-if="!isLastView()" @click="closeRightTags"><right style="width: 1em; height: 1em;" />关闭右侧</li>
      <li @click="closeAllTags(selectedTag)"><circle-close style="width: 1em; height: 1em;" />全部关闭</li>
    </ul>
  </div>
</template>

<script setup>
import ScrollPane from './ScrollPane'
import { getNormalPath } from '@/utils/common'
import useTagsViewStore from '@/store/modules/tagsView'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

const visible = ref(false)
const top = ref(0)
const left = ref(0)
const selectedTag = ref({})
const affixTags = ref([])
const scrollPaneRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const isFullscreen = ref(false)
const hiddenElements = ref([])

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const settingsStore = useSettingsStore()

const visitedViews = computed(() => useTagsViewStore().visitedViews)
const routes = computed(() => usePermissionStore().routes)
const theme = computed(() => useSettingsStore().theme)
const tagsIcon = computed(() => useSettingsStore().tagsIcon)
const tagsViewPersist = computed(() => useSettingsStore().tagsViewPersist)

// 下拉菜单针对当前激活的 tag
const selectedDropdownTag = computed(() => visitedViews.value.find(v => isActive(v)) || {})

watch(route, () => {
  addTags()
  moveToCurrentTag()
})

watch(visible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeMenu)
  } else {
    document.body.removeEventListener('click', closeMenu)
  }
})

watch(visitedViews, () => {
  nextTick(() => updateArrowState())
})

onMounted(() => {
  initTags()
  addTags()
  window.addEventListener('resize', updateArrowState)
  window.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateArrowState)
  window.removeEventListener('keydown', handleKeyDown)
})

function handleKeyDown(event) {
  // 当按下Esc键且处于全屏状态时，退出全屏
  if (event.key === 'Escape' && isFullscreen.value) {
    toggleFullscreen()
  }
}

function isActive(r) {
  return r.path === route.path
}

function activeStyle(tag) {
  if (!isActive(tag)) return {}
  return {
    'background-color': theme.value,
    'border-color': theme.value
  }
}

function isAffix(tag) {
  return tag && tag.meta && tag.meta.affix
}

function isFirstView() {
  try {
    const tag = selectedTag.value && selectedTag.value.fullPath ? selectedTag.value : selectedDropdownTag.value
    return tag.fullPath === '/index' || tag.fullPath === visitedViews.value[1].fullPath
  } catch (err) {
    return false
  }
}

function isLastView() {
  try {
    const tag = selectedTag.value && selectedTag.value.fullPath ? selectedTag.value : selectedDropdownTag.value
    return tag.fullPath === visitedViews.value[visitedViews.value.length - 1].fullPath
  } catch (err) {
    return false
  }
}

function filterAffixTags(routes, basePath = '') {
  let tags = []
  routes.forEach(route => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + '/' + route.path)
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: { ...route.meta }
      })
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, route.path)
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags]
      }
    }
  })
  return tags
}

function initTags() {
  if (tagsViewPersist.value) {
    useTagsViewStore().loadPersistedViews()
  }
  const res = filterAffixTags(routes.value)
  affixTags.value = res
  for (const tag of res) {
    if (tag.name) {
      useTagsViewStore().addAffixView(tag)
    }
  }
}

function addTags() {
  const { name } = route
  if (name) {
    useTagsViewStore().addView(route)
  }
}

function moveToCurrentTag() {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        scrollPaneRef.value.moveToTarget(r)
        if (r.fullPath !== route.fullPath) {
          useTagsViewStore().updateVisitedView(route)
        }
      }
    }
  })
}

function scrollLeft() {
  if (!canScrollLeft.value) return
  scrollPaneRef.value.scrollToStart()
}

function scrollRight() {
  if (!canScrollRight.value) return
  scrollPaneRef.value.scrollToEnd()
}

function updateArrowState() {
  nextTick(() => {
    if (scrollPaneRef.value) {
      const state = scrollPaneRef.value.getScrollState()
      canScrollLeft.value = state.canLeft
      canScrollRight.value = state.canRight
    }
  })
}

function toggleFullscreen() {
  const mainContainer = document.querySelector('.main-container')
  const navbar = document.querySelector('.navbar')
  const sidebar = document.querySelector('.sidebar-container')
  if (!mainContainer) return

  if (!isFullscreen.value) {
    mainContainer.classList.add('fullscreen-mode')
    document.body.style.overflow = 'hidden'
    const elementsToHide = [{ el: navbar, originalDisplay: navbar?.style.display || '' }, { el: sidebar, originalDisplay: sidebar?.style.display || '' }]
    elementsToHide.forEach(item => {
      if (item.el && item.el.style.display !== 'none') {
        item.originalDisplay = item.el.style.display
        item.el.style.display = 'none'
        hiddenElements.value.push(item)
      }
    })
    isFullscreen.value = true
  } else {
    mainContainer.classList.remove('fullscreen-mode')
    document.body.style.overflow = ''
    hiddenElements.value.forEach(item => {
      if (item.el) {
        item.el.style.display = item.originalDisplay
      }
    })
    hiddenElements.value = []
    document.querySelector('.tags-action-btn').blur()
    isFullscreen.value = false
  }
}

function handleDropdownCommand(command) {
  const tag = selectedDropdownTag.value
  selectedTag.value = tag
  switch (command) {
    case 'refresh':     refreshSelectedTag(tag); break
    case 'fullscreen':  toggleFullscreen(); break
    case 'close':       closeSelectedTag(tag); break
    case 'closeOthers': closeOthersTags(); break
    case 'closeLeft':   closeLeftTags(); break
    case 'closeRight':  closeRightTags(); break
    case 'closeAll':    closeAllTags(tag); break
  }
}

function refreshSelectedTag(view) {
  proxy.$tab.refreshPage(view)
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route)
  }
}

function closeSelectedTag(view) {
  proxy.$tab.closePage(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view)
    }
  })
}

function closeRightTags() {
  proxy.$tab.closeRightPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}

function closeLeftTags() {
  proxy.$tab.closeLeftPage(selectedTag.value).then(visitedViews => {
    if (!visitedViews.find(i => i.fullPath === route.fullPath)) {
      toLastView(visitedViews)
    }
  })
}

function closeOthersTags() {
  router.push(selectedTag.value).catch(() => { })
  proxy.$tab.closeOtherPage(selectedTag.value).then(() => {
    moveToCurrentTag()
  })
}

function closeAllTags(view) {
  proxy.$tab.closeAllPage().then(({ visitedViews }) => {
    if (affixTags.value.some(tag => tag.path === route.path)) {
      return
    }
    toLastView(visitedViews, view)
  })
}

function toLastView(visitedViews, view) {
  const latestView = visitedViews.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath)
  } else {
    if (view && view.name === 'Dashboard') {
      router.replace({ path: '/redirect' + view.fullPath })
    } else {
      router.push('/')
    }
  }
}

function openMenu(tag, e) {
  left.value = e.clientX
  top.value = e.clientY
  visible.value = true
  selectedTag.value = tag
}

function closeMenu() {
  visible.value = false
}

function handleScroll() {
  closeMenu()
  updateArrowState()
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 40px;
  width: 100%;
  /* 与导航栏同色，组成顶部白色横带 */
  background: var(--navbar-bg);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  backdrop-filter: blur(12px) saturate(160%);
  border-bottom: 1px solid rgba(38, 35, 54, 0.05);
  display: flex;
  align-items: center;
  overflow: hidden;

  $btn-width: 28px;
  $divider: 1px solid var(--tags-item-border, #eceaf4);

  .tags-nav-btn {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: $btn-width;
    height: 40px;
    cursor: pointer;
    color: var(--text-secondary, #8b8899);
    font-size: 13px;
    user-select: none;
    transition: background 0.2s ease, color 0.2s ease;

    &:hover:not(.disabled) {
      background: rgba(240, 67, 110, 0.06);
      color: var(--el-color-primary, #f0436e);
    }

    &.disabled {
      color: var(--text-placeholder, #b6b3c2);
      cursor: not-allowed;
    }

    &--left  { border-right: $divider; }
    &--right { border-left: $divider; }
  }

  .tags-view-wrapper {
    flex: 1;
    min-width: 0;
    height: 100%;

    .tags-view-item {
      /* inline-flex：让内部 svg-icon / 文字 / 关闭按钮垂直居中对齐，
       * 配合 vertical-align: middle 在父 line-height: 40px 容器中垂直居中。
       * 此前用 inline-block + baseline 对齐，导致 close 图标视觉偏下被挤。 */
      display: inline-flex;
      align-items: center;
      gap: 4px;
      vertical-align: middle;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 1;
      border: 1px solid var(--tags-item-border, #eceaf4);
      color: var(--tags-item-text, #4b4861);
      background: var(--tags-item-bg, rgba(255, 255, 255, 0.8));
      padding: 0 10px;
      font-size: 12px;
      margin-left: 5px;
      border-radius: var(--radius-full, 999px);
      transition: all 0.25s ease;
      white-space: nowrap;

      &:first-of-type { margin-left: 6px; }
      &:last-of-type  { margin-right: 15px; }

      &:hover {
        color: var(--el-color-primary, #f0436e);
        border-color: rgba(240, 67, 110, 0.25);
        background: rgba(240, 67, 110, 0.04);
      }

      &.active {
        background-color: var(--el-color-primary, #f0436e);
        color: #fff;
        border-color: var(--el-color-primary, #f0436e);
        box-shadow: 0 2px 6px rgba(240, 67, 110, 0.25);

        &:hover {
          background-color: #d9305c;
          border-color: #d9305c;
          color: #fff;
        }

        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 1px;
          flex-shrink: 0;
        }
      }

      /* 关闭按钮：inline-flex 让 SVG 居中，避免 baseline 偏移 */
      .tags-close-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 16px;
        height: 16px;
        margin-left: 2px;
        border-radius: 50%;
        flex-shrink: 0;
        transition: background 0.2s ease, transform 0.2s ease;

        .el-icon-close {
          width: 12px;
          height: 12px;
        }

        &:hover {
          background: rgba(255, 255, 255, 0.25);
          transform: scale(1.1);
        }
      }

      /* 激活态下关闭按钮 hover 用白色半透明，更醒目 */
      &.active .tags-close-btn:hover {
        background: rgba(255, 255, 255, 0.35);
      }

      /* 非激活态下关闭按钮 hover 用玫红浅染 */
      &:not(.active) .tags-close-btn:hover {
        background: rgba(240, 67, 110, 0.12);
        color: var(--el-color-primary, #f0436e);
      }
    }
  }

  .tags-view-item.active.has-icon::before {
    content: none !important;
  }

  .tags-action-dropdown {
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  .tags-action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: $btn-width;
    height: 40px;
    cursor: pointer;
    color: var(--text-secondary, #8b8899);
    font-size: 13px;
    border-left: $divider;
    user-select: none;
    transition: background 0.2s ease, color 0.2s ease;

    &:hover {
      background: rgba(240, 67, 110, 0.06);
      color: var(--el-color-primary, #f0436e);
    }
  }

  .tags-refresh-btn {
    width: 60px;
    gap: 4px;
    padding: 0 8px;
  }

  .contextmenu {
    margin: 0;
    background: var(--el-bg-color-overlay, #fff);
    z-index: 3000;
    position: fixed;
    list-style-type: none;
    padding: 6px;
    border-radius: var(--radius-sm, 8px);
    font-size: 13px;
    font-weight: 400;
    color: var(--text-regular, #4b4861);
    /* 柔和紫调阴影，禁硬阴影 */
    box-shadow: var(--shadow-lg, 0 12px 32px rgba(124, 116, 160, 0.14));
    border: 1px solid var(--card-border, #eceaf4);

    li {
      margin: 0;
      padding: 8px 14px;
      cursor: pointer;
      border-radius: var(--radius-xs, 6px);
      transition: background 0.15s ease, color 0.15s ease;
      display: flex;
      align-items: center;
      gap: 6px;

      &:hover {
        background: rgba(240, 67, 110, 0.08);
        color: var(--el-color-primary, #f0436e);
      }
    }
  }
}
</style>

<style lang="scss">
/* 旧版 .tags-view-item .el-icon-close 样式已废弃，
 * 关闭按钮的样式由 scoped 块中的 .tags-close-btn 统一处理。
 * 旧样式硬编码 vertical-align: 2px + hover 缩小尺寸，与 inline-flex 布局冲突。 */

/* 页签全屏模式样式 */
.main-container.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.main-container.fullscreen-mode .fixed-header {
  display: block !important;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  width: 100% !important;
  z-index: 1000;
}

.main-container.fullscreen-mode .fixed-header .navbar {
  display: none !important;
}

.main-container.fullscreen-mode .app-main {
  position: fixed;
  top: 40px;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0 !important;
  padding: 0 !important;
  height: calc(100vh - 40px) !important;
  min-height: calc(100vh - 40px) !important;
  overflow: auto;
}
</style>
