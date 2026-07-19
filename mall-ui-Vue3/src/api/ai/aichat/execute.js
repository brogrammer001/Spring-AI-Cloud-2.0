import { ElMessage } from 'element-plus'

export function handleRouteJump(componentPath, { proxy, router }) {
  console.log(componentPath)
  // 从全局路由中查找对应的路由信息
  const allRoutes = router.getRoutes()
  console.log(allRoutes)
  let targetRoute = null
  
  // 1. 尝试直接匹配路由路径
  if (componentPath.startsWith('/')) {
    for (const route of allRoutes) {
      if (route.path === componentPath) {
        targetRoute = route
        break
      }
    }
  }
  
  // 2. 如果是组件路径（如 system/post/index），查找对应的路由
  if (!targetRoute) {
    for (const route of allRoutes) {
      // 递归查找路由的 component 或 children
      if (findRouteByComponent(route, componentPath)) {
        targetRoute = findRouteByComponent(route, componentPath)
        break
      }
    }
  }
  
  // 3. 如果找到匹配的路由，打开标签页
  if (targetRoute) {
    const pageTitle = targetRoute.meta?.title || 'AI 推荐页面'
    proxy.$tab.openPage(pageTitle, targetRoute.path)
  } else {
    // 4. 如果仍然找不到，尝试转换为路由路径格式后查找
    const possiblePath = '/' + componentPath.replace(/\/index$/, '')
    for (const route of allRoutes) {
      if (route.path === possiblePath) {
        targetRoute = route
        break
      }
    }
    
    if (targetRoute) {
      const pageTitle = targetRoute.meta?.title || 'AI 推荐页面'
      proxy.$tab.openPage(pageTitle, targetRoute.path)
    } else {
      // 5. 所有尝试都失败，显示错误提示
      ElMessage.error("AI 找到了路径，但当前账号可能没有该页面的访问权限")
    }
  }
}

// 递归查找路由（根据组件路径）
function findRouteByComponent(route, componentPath) {
  // 检查当前路由
  if (route.meta?.component === componentPath) {
    return route
  }
  
  // 检查子路由
  if (route.children && route.children.length > 0) {
    for (const childRoute of route.children) {
      const found = findRouteByComponent(childRoute, componentPath)
      if (found) {
        return found
      }
    }
  }
  
  return null
}