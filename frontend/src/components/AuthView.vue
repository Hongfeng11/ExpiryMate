<script setup>
import { ref } from 'vue'
import { signIn, signUp } from '../services/auth'

const emit = defineEmits(['authenticated'])
const mode = ref('login')
const email = ref('')
const password = ref('')
const displayName = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  busy.value = true
  try {
    const user = mode.value === 'login'
      ? await signIn({ email: email.value, password: password.value })
      : await signUp({ email: email.value, password: password.value, displayName: displayName.value })
    emit('authenticated', user)
  } catch (err) { error.value = err.message || '暂时无法完成操作' }
  finally { busy.value = false }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="auth-brand"><span>◈</span><div><strong>ExpiryMate</strong><small>物品保质期管理</small></div></div>
      <p class="auth-eyebrow">让家庭物品一起管理</p>
      <h1>{{ mode === 'login' ? '欢迎回来' : '创建你的账号' }}</h1>
      <p class="auth-intro">登录后创建共享清单，邀请家人一起记录和查看物品。</p>
      <form @submit.prevent="submit">
        <label v-if="mode === 'register'">显示名称<input v-model.trim="displayName" autocomplete="name" maxlength="80" required placeholder="例如：小林" /></label>
        <label>邮箱<input v-model.trim="email" type="email" autocomplete="email" maxlength="254" required placeholder="name@example.com" /></label>
        <label>密码<input v-model="password" type="password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" minlength="8" maxlength="72" required placeholder="至少 8 位" /></label>
        <p v-if="error" class="auth-error" role="alert">{{ error }}</p>
        <button class="auth-submit" :disabled="busy">{{ busy ? '请稍候…' : mode === 'login' ? '登录' : '注册并进入' }}</button>
      </form>
      <p class="auth-switch">{{ mode === 'login' ? '还没有账号？' : '已经有账号？' }}
        <button type="button" @click="mode = mode === 'login' ? 'register' : 'login'; error = ''">{{ mode === 'login' ? '立即注册' : '返回登录' }}</button>
      </p>
    </section>
  </main>
</template>

<style scoped>
.auth-page{min-height:100vh;display:grid;place-items:center;padding:24px;background:radial-gradient(circle at 15% 12%,#dff4e8 0,transparent 32%),linear-gradient(135deg,#f5f7fb,#edf3f3)}
.auth-card{width:min(100%,430px);padding:38px;border:1px solid #e9edf2;border-radius:24px;background:#fff;box-shadow:0 22px 65px #28384916}
.auth-brand{display:flex;align-items:center;gap:11px;color:#263e35}.auth-brand>span{display:grid;place-items:center;width:42px;height:42px;border-radius:14px;background:#e4f5e9;color:#44855c;font-size:24px}.auth-brand strong,.auth-brand small{display:block}.auth-brand strong{font-size:17px}.auth-brand small{margin-top:3px;color:#829087;font-size:11px}
.auth-eyebrow{margin:35px 0 8px;color:#4f966a;font-size:12px;font-weight:700;letter-spacing:.08em}.auth-card h1{margin:0;color:#26352d;font-size:27px}.auth-intro{margin:10px 0 25px;color:#819088;font-size:13px;line-height:1.7}
form{display:grid;gap:15px}label{display:grid;gap:7px;color:#506057;font-size:12px;font-weight:600}input{height:44px;padding:0 13px;border:1px solid #e3e9e5;border-radius:11px;outline:none;color:#26352d;font:inherit}input:focus{border-color:#6ba77e;box-shadow:0 0 0 3px #6ba77e1c}.auth-submit{height:45px;margin-top:5px;border:0;border-radius:11px;background:#548b67;color:#fff;font:inherit;font-weight:700;cursor:pointer}.auth-submit:disabled{opacity:.6;cursor:wait}.auth-error{margin:0;color:#bb5c52;font-size:12px}.auth-switch{margin:20px 0 0;text-align:center;color:#849087;font-size:12px}.auth-switch button{padding:4px;border:0;background:none;color:#4c8b63;font:inherit;font-weight:700;cursor:pointer}
@media(max-width:500px){.auth-card{padding:27px 22px;border-radius:19px}}
</style>
