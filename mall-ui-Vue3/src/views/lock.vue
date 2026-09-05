<template>
  <div class="lock-container">
    <!-- 动态粒子背景 -->
    <canvas ref="particleCanvas" class="particle-bg"></canvas>

    <!-- 时钟 -->
    <div class="lock-time">{{ currentTime }}</div>
    <div class="lock-date">{{ currentDate }}</div>

    <!-- 锁屏卡片 -->
    <div class="lock-card">
      <div class="avatar-wrap">
        <img :src="userStore.avatar" class="lock-avatar" @error="onAvatarError" />
        <div class="lock-icon"><i class="fas fa-lock"></i></div>
      </div>
      <div class="lock-username">{{ userStore.nickName }}</div>
      <div class="lock-hint">系统已锁定，请输入密码解锁</div>

      <div class="input-wrap" :class="{ shake: isShaking }">
        <input ref="passwordInput" v-model="password" type="password" placeholder="请输入登录密码" class="lock-input" @keydown.enter="handleUnlock" autocomplete="off" />
        <button class="unlock-btn" @click="handleUnlock" :disabled="loading">
          <span v-if="!loading"><i class="fas fa-arrow-right"></i></span>
          <span v-else class="loading-dot">···</span>
        </button>
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <div class="lock-footer">
        <a href="javascript:;" @click="goLogin">退出重新登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import { unlockScreen } from '@/api/login'
import defAva from '@/assets/images/profile.jpg'

const router = useRouter()
const userStore = useUserStore()
const lockStore = useLockStore()

const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const isShaking = ref(false)
const currentTime = ref('')
const currentDate = ref('')
const passwordInput = ref(null)
const particleCanvas = ref(null)

let timer = null
let animationId = null
let particles = []

const onAvatarError = (e) => {
  e.target.src = defAva
}

const startClock = () => {
  const update = () => {
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
    const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
  }
  update()
  timer = setInterval(update, 1000)
}

const handleUnlock = async () => {
  if (!password.value) {
    showError('请输入密码')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await unlockScreen(password.value)
    const lockPath = lockStore.lockPath
    lockStore.unlockScreen()
    router.replace(lockPath)
  } catch (err) {
    const msg = err.message || err.toString()
    showError(msg)
    password.value = ''
    nextTick(() => passwordInput.value?.focus())
  } finally {
    loading.value = false
  }
}

const showError = (msg) => {
  errorMsg.value = msg
  isShaking.value = true
  setTimeout(() => { isShaking.value = false }, 600)
}

const goLogin = () => {
  lockStore.unlockScreen()
  userStore.logOut().then(() => {
    router.push('/login')
  })
}

const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  // 马卡龙三色粒子（玫红 / 紫 / 蓝）
  const palette = [
    { dot: '240, 67, 110', line: '240, 67, 110' },
    { dot: '139, 92, 246', line: '167, 139, 250' },
    { dot: '96, 165, 250', line: '96, 165, 250' }
  ]

  particles = Array.from({ length: 70 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    r: Math.random() * 2.5 + 1,
    dx: (Math.random() - 0.5) * 0.5,
    dy: (Math.random() - 0.5) * 0.5,
    alpha: Math.random() * 0.35 + 0.12,
    color: palette[Math.floor(Math.random() * palette.length)]
  }))

  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach(p => {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(${p.color.dot},${p.alpha})`
      ctx.fill()
      p.x += p.dx
      p.y += p.dy
      if (p.x < 0 || p.x > canvas.width) p.dx *= -1
      if (p.y < 0 || p.y > canvas.height) p.dy *= -1
    })
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i], b = particles[j]
        const dist = Math.hypot(a.x - b.x, a.y - b.y)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.strokeStyle = `rgba(${a.color.line},${0.10 * (1 - dist / 120)})`
          ctx.lineWidth = 0.6
          ctx.stroke()
        }
      }
    }
    animationId = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  startClock()
  initParticles()
  nextTick(() => passwordInput.value?.focus())
})

onBeforeUnmount(() => {
  clearInterval(timer)
  cancelAnimationFrame(animationId)
})
</script>

<style scoped>
/* ===== 淡紫马卡龙风：浅色薰衣草底 + 毛玻璃白卡（禁止深色背景） ===== */
.lock-container {
  position: fixed;
  inset: 0;
  background:
    radial-gradient(at 12% 12%, rgba(250, 214, 226, 0.55) 0px, transparent 42%),
    radial-gradient(at 88% 18%, rgba(221, 214, 246, 0.55) 0px, transparent 42%),
    radial-gradient(at 80% 92%, rgba(214, 228, 250, 0.50) 0px, transparent 42%),
    #f4f2f9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  overflow: hidden;
}

.particle-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.lock-time {
  position: relative;
  z-index: 1;
  font-size: 72px;
  font-weight: 200;
  color: #262336;
  letter-spacing: 4px;
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.lock-date {
  position: relative;
  z-index: 1;
  font-size: 15px;
  color: #8b8899;
  margin-bottom: 40px;
  letter-spacing: 2px;
}

.lock-card {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 40px 48px;
  width: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow:
    0 1px 2px rgba(17, 24, 39, 0.04),
    0 16px 40px rgba(124, 116, 160, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  animation: cardIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.avatar-wrap {
  position: relative;
  margin-bottom: 16px;
}

.lock-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  box-shadow: 0 0 0 3px rgba(240, 67, 110, 0.25), 0 6px 16px rgba(124, 116, 160, 0.18);
  object-fit: cover;
  display: block;
}

.lock-icon {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #f0436e 0%, #ff7a9c 100%);
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #ffffff;
  box-shadow: 0 3px 8px rgba(240, 67, 110, 0.35);
}

.lock-username {
  color: #262336;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.lock-hint {
  color: #8b8899;
  font-size: 13px;
  margin-bottom: 28px;
}

.input-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #eceaf4;
  border-radius: 999px;
  padding: 4px 4px 4px 20px;
  transition: border-color 0.3s, box-shadow 0.3s, background 0.3s;
}

.input-wrap:focus-within {
  border-color: #f0436e;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(240, 67, 110, 0.12);
}

.input-wrap.shake {
  animation: shake 0.5s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(6px); }
}

.lock-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #262336;
  font-size: 15px;
  padding: 10px 0;
}

.lock-input::placeholder {
  color: #b6b3c2;
}

.unlock-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f0436e 0%, #ff7a9c 100%);
  border: none;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(240, 67, 110, 0.30);
}

.unlock-btn:hover:not(:disabled) {
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 8px 20px rgba(240, 67, 110, 0.38);
}

.unlock-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-dot {
  font-size: 13px;
  letter-spacing: 1px;
}

.error-msg {
  margin-top: 14px;
  color: #d9305c;
  font-size: 13px;
  text-align: center;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to   { opacity: 1; transform: translateY(0); }
}

.lock-footer {
  margin-top: 24px;
}

.lock-footer a {
  color: #8b8899;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}

.lock-footer a:hover {
  color: #f0436e;
}
</style>
