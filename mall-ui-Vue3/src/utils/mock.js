/**
 * 离线演示模式（不连接后端接口也能进入系统）
 * 开关：.env 中 VITE_APP_USE_BACKEND = false 时启用
 */

// 是否启用离线演示模式（VITE_APP_USE_BACKEND = false 时不连后端）
export function isMockEnabled() {
  return import.meta.env.VITE_APP_USE_BACKEND === 'false'
}

const MOCK_TOKEN = 'mock-token-offline-demo'

// 离线模式下的静态菜单（组件路径对应 src/views 下的页面）
const mockMenus = [
  {
    name: 'System',
    path: '/system',
    component: 'Layout',
    redirect: 'noRedirect',
    alwaysShow: true,
    meta: { title: '系统管理', icon: 'system' },
    children: [
      { name: 'User', path: 'user', component: 'system/user/index', meta: { title: '用户管理', icon: 'user' } },
      { name: 'Role', path: 'role', component: 'system/role/index', meta: { title: '角色管理', icon: 'peoples' } },
      { name: 'Menu', path: 'menu', component: 'system/menu/index', meta: { title: '菜单管理', icon: 'tree-table' } },
      { name: 'Dept', path: 'dept', component: 'system/dept/index', meta: { title: '部门管理', icon: 'tree' } },
      { name: 'Post', path: 'post', component: 'system/post/index', meta: { title: '岗位管理', icon: 'post' } },
      { name: 'Dict', path: 'dict', component: 'system/dict/index', meta: { title: '字典管理', icon: 'dict' } },
      { name: 'Config', path: 'config', component: 'system/config/index', meta: { title: '参数设置', icon: 'edit' } },
      { name: 'Notice', path: 'notice', component: 'system/notice/index', meta: { title: '通知公告', icon: 'message' } },
      {
        name: 'Log',
        path: 'log',
        component: 'ParentView',
        alwaysShow: true,
        meta: { title: '日志管理', icon: 'log' },
        children: [
          { name: 'Operlog', path: 'operlog', component: 'system/operlog/index', meta: { title: '操作日志', icon: 'form' } },
          { name: 'Logininfor', path: 'logininfor', component: 'system/logininfor/index', meta: { title: '登录日志', icon: 'logininfor' } }
        ]
      }
    ]
  },
  {
    name: 'Monitor',
    path: '/monitor',
    component: 'Layout',
    redirect: 'noRedirect',
    alwaysShow: true,
    meta: { title: '系统监控', icon: 'monitor' },
    children: [
      { name: 'Online', path: 'online', component: 'monitor/online/index', meta: { title: '在线用户', icon: 'online' } },
      { name: 'Job', path: 'job', component: 'monitor/job/index', meta: { title: '定时任务', icon: 'job' } }
    ]
  },
  {
    name: 'Tool',
    path: '/tool',
    component: 'Layout',
    redirect: 'noRedirect',
    alwaysShow: true,
    meta: { title: '系统工具', icon: 'tool' },
    children: [
      { name: 'Build', path: 'build', component: 'tool/build/index', meta: { title: '表单构建', icon: 'build' } },
      { name: 'Gen', path: 'gen', component: 'tool/gen/index', meta: { title: '代码生成', icon: 'code' } }
    ]
  }
]

// 关键接口 mock 数据（与响应拦截器返回体结构一致：res = body）
function matchMock(config) {
  const url = (config.url || '').split('?')[0]
  const method = (config.method || 'get').toLowerCase()
  const ok = (body) => ({ code: 200, msg: '操作成功', ...body })

  if (method === 'post' && url === '/auth/login') {
    return ok({ data: { access_token: MOCK_TOKEN } })
  }
  if (method === 'delete' && url === '/auth/logout') {
    return ok({})
  }
  if (method === 'get' && url === '/code') {
    // 离线模式关闭验证码
    return ok({ captchaEnabled: false, img: '', uuid: '' })
  }
  if (method === 'get' && url === '/system/user/getInfo') {
    return ok({
      user: { userId: 1, userName: 'admin', nickName: '离线演示', avatar: '' },
      roles: ['admin'],
      permissions: ['*:*:*'],
      isDefaultModifyPwd: false,
      isPasswordExpired: false
    })
  }
  if (method === 'get' && url === '/system/menu/getRouters') {
    return ok({ data: mockMenus })
  }
  return null
}

// 未命中关键接口时的通用兜底响应（保证页面不崩溃）
function fallbackMock() {
  return {
    code: 200,
    msg: '离线演示模式',
    data: {},
    rows: [],
    total: 0
  }
}

// 模拟一次请求：返回与响应拦截器成功分支一致的 body
export function mockRequest(config) {
  const matched = matchMock(config)
  return matched || fallbackMock()
}
