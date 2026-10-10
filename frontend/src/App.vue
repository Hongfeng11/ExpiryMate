<script setup>
import { computed, onMounted, ref } from 'vue'
import { createItem, isApiMode, listItems, removeItem, updateItem } from './services/items'

const items = ref([])
const selectedNav = ref('overview')
const activeFilter = ref('all')
const search = ref('')
const categoryFilter = ref('全部分类')
const showModal = ref(false)
const showNotifications = ref(false)
const loading = ref(true)
const errorMessage = ref('')
const sortDirection = ref('asc')
const notificationRead = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return { name: '', category: '食品', location: '', quantity: 1, unit: '件', manufacturedOn: '', purchasedOn: '', shelfLifeValue: '', shelfLifeUnit: '天', expiresOn: '', notes: '' }
}
function parseDate(value) {
  if (!value) return null
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, m - 1, d)
}
function daysLeft(value) {
  const target = parseDate(value)
  if (!target) return null
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((target - today) / 86400000)
}
function itemState(item) {
  if (['done', 'used_up', 'discarded'].includes(String(item.status || '').toLowerCase())) return 'done'
  const days = daysLeft(item.expiresOn || item.expires_on)
  if (days < 0) return 'expired'
  if (days <= 7) return 'soon'
  return 'normal'
}
function stateLabel(item) {
  const state = itemState(item)
  if (state === 'done') return '已处理'
  if (state === 'expired') return '已过期'
  if (state === 'soon') {
    const days = daysLeft(item.expiresOn || item.expires_on)
    return days === 0 ? '今日到期' : `还剩 ${days} 天`
  }
  return '状态正常'
}
function dateLabel(value) {
  if (!value) return '—'
  const d = parseDate(value)
  return `${d.getMonth() + 1}月${String(d.getDate()).padStart(2, '0')}日`
}
const activeItems = computed(() => items.value.filter(item => item.status === 'active' || !item.status))
const expiringCount = computed(() => activeItems.value.filter(item => itemState(item) === 'soon').length)
const expiredCount = computed(() => activeItems.value.filter(item => itemState(item) === 'expired').length)
const currentAlerts = computed(() => activeItems.value.filter(item => ['soon', 'expired'].includes(itemState(item))).sort((a, b) => daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)))
const upcomingItems = computed(() => activeItems.value.filter(item => itemState(item) === 'soon').sort((a, b) => daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)))
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase()
  return items.value.filter(item => {
    const state = itemState(item)
    const statusOk = activeFilter.value === 'all' || (activeFilter.value === 'soon' && state === 'soon') || (activeFilter.value === 'expired' && state === 'expired') || (activeFilter.value === 'normal' && state === 'normal')
    const categoryOk = categoryFilter.value === '全部分类' || item.category === categoryFilter.value
    const queryOk = !q || [item.name, item.category, item.location].some(value => (value || '').toLowerCase().includes(q))
    return statusOk && categoryOk && queryOk
  }).sort((a, b) => (daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)) * (sortDirection.value === 'asc' ? 1 : -1))
})
const categoryOptions = computed(() => ['全部分类', ...new Set(items.value.map(item => item.category).filter(Boolean))])
const unreadNotifications = computed(() => currentAlerts.value.length)

async function refresh() {
  loading.value = true
  errorMessage.value = ''
  try { items.value = await listItems() }
  catch (error) { errorMessage.value = error.message || '暂时无法连接服务'; items.value = [] }
  finally { loading.value = false }
}
function openAdd() { editingId.value = null; form.value = emptyForm(); errorMessage.value = ''; showModal.value = true }
function openEdit(item) {
  editingId.value = item.id
  form.value = { ...emptyForm(), ...item, manufacturedOn: item.manufacturedOn || item.manufactured_on || '', purchasedOn: item.purchasedOn || item.purchased_on || '', expiresOn: item.expiresOn || item.expires_on || '' }
  errorMessage.value = ''
  showModal.value = true
}
function addMonths(date, months) {
  const result = new Date(date)
  const day = result.getDate()
  result.setDate(1)
  result.setMonth(result.getMonth() + Number(months))
  const lastDay = new Date(result.getFullYear(), result.getMonth() + 1, 0).getDate()
  result.setDate(Math.min(day, lastDay))
  return result
}
function dateString(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function calculatedExpiry() {
  if (form.value.expiresOn) return form.value.expiresOn
  if (!form.value.manufacturedOn || !form.value.shelfLifeValue) return ''
  const base = parseDate(form.value.manufacturedOn)
  const amount = Number(form.value.shelfLifeValue)
  if (form.value.shelfLifeUnit === '天') base.setDate(base.getDate() + amount)
  if (form.value.shelfLifeUnit === '月') return dateString(addMonths(base, amount))
  if (form.value.shelfLifeUnit === '年') return dateString(addMonths(base, amount * 12))
  return dateString(base)
}
async function saveItem() {
  errorMessage.value = ''
  if (!form.value.name.trim()) { errorMessage.value = '请填写物品名称'; return }
  const expiresOn = calculatedExpiry()
  if (!expiresOn) { errorMessage.value = '请填写到期日，或填写生产日期和保质期'; return }
  saving.value = true
  try {
    const payload = { ...form.value, name: form.value.name.trim(), expiresOn, quantity: Number(form.value.quantity) || 1, emoji: emojiFor(form.value.category), color: colorFor(form.value.category), status: 'active' }
    if (editingId.value) await updateItem(editingId.value, payload)
    else await createItem(payload)
    showModal.value = false
    await refresh()
  } catch (error) { errorMessage.value = error.message || '保存失败，请稍后重试' }
  finally { saving.value = false }
}
function emojiFor(category) { return ({ 食品: '🥬', 个护: '🧴', 保健品: '💊', 清洁: '🧽' })[category] || '📦' }
function colorFor(category) { return ({ 食品: 'green', 个护: 'blue', 保健品: 'violet', 清洁: 'cyan' })[category] || 'amber' }
async function markDone(item) {
  try { await updateItem(item.id, { status: 'done' }); await refresh() }
  catch (error) { errorMessage.value = error.message || '更新失败' }
}
async function deleteItem(item) {
  if (!window.confirm(`确定删除「${item.name}」吗？`)) return
  try { await removeItem(item.id); await refresh() }
  catch (error) { errorMessage.value = error.message || '删除失败' }
}
function toggleSort() { sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc' }
function setFilter(filter) { activeFilter.value = filter; selectedNav.value = filter === 'all' ? 'overview' : 'items' }
function showExpiryList() { selectedNav.value = 'items'; activeFilter.value = 'soon' }
function displayName(item) { return item.name || '未命名物品' }
onMounted(refresh)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">◈</span><span>ExpiryMate<small>物品保质期管理</small></span></div>
      <div class="workspace-label">工作空间</div>
      <nav class="nav-list">
        <button :class="['nav-link', selectedNav === 'overview' ? 'active' : '']" @click="selectedNav='overview'; activeFilter='all'"><span class="nav-icon">▦</span>总览</button>
        <button :class="['nav-link', selectedNav === 'items' ? 'active' : '']" @click="selectedNav='items'"><span class="nav-icon">▤</span>物品清单<span class="nav-count">{{ items.length }}</span></button>
        <button class="nav-link" @click="showNotifications=!showNotifications"><span class="nav-icon">◷</span>临期提醒<span v-if="unreadNotifications" class="nav-dot"></span></button>
      </nav>
      <div class="sidebar-divider"></div>
      <div class="workspace-label">快捷分类</div>
      <button class="category-link" @click="categoryFilter='食品'; selectedNav='items'"><i class="category-dot food"></i>食品 <span>{{ items.filter(i=>i.category==='食品').length }}</span></button>
      <button class="category-link" @click="categoryFilter='个护'; selectedNav='items'"><i class="category-dot personal"></i>个护 <span>{{ items.filter(i=>i.category==='个护').length }}</span></button>
      <button class="category-link" @click="categoryFilter='保健品'; selectedNav='items'"><i class="category-dot health"></i>保健品 <span>{{ items.filter(i=>i.category==='保健品').length }}</span></button>
      <div class="sidebar-bottom">
        <div class="tip-card"><div class="tip-icon">✦</div><strong>少一点浪费</strong><p>让每一件物品都在新鲜时被好好使用。</p></div>
        <div class="profile"><div class="avatar">林</div><div><strong>我的家庭</strong><small>个人空间</small></div><button aria-label="设置">•••</button></div>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="breadcrumb">ExpiryMate <span>/</span> {{ selectedNav === 'items' ? '物品清单' : '总览' }}</div>
        <div class="top-actions">
          <label class="global-search"><span>⌕</span><input v-model="search" placeholder="搜索物品、分类或位置" /><kbd>⌘ K</kbd></label>
          <button class="icon-button notification-button" aria-label="通知" @click="showNotifications=!showNotifications">♧<span v-if="unreadNotifications" class="notification-badge"></span></button>
          <div class="top-avatar">林</div>
        </div>
        <div v-if="showNotifications" class="notification-popover">
          <div class="popover-title">需要关注 <span>{{ currentAlerts.length }} 件</span></div>
          <button v-for="item in currentAlerts.slice(0,4)" :key="item.id" class="popover-item" @click="selectedNav='items'; showNotifications=false">
            <span class="mini-emoji">{{ item.emoji || '📦' }}</span><span><b>{{ displayName(item) }}</b><small>{{ stateLabel(item) }} · {{ dateLabel(item.expiresOn || item.expires_on) }}到期</small></span><span class="chevron">›</span>
          </button>
          <div v-if="!currentAlerts.length" class="popover-empty">目前没有待处理提醒</div>
          <button class="popover-footer" @click="showExpiryList(); showNotifications=false">查看全部提醒 →</button>
        </div>
      </header>

      <section class="page-content">
        <div class="welcome-row">
          <div><div class="eyebrow"><span class="live-dot"></span>你的家庭物品状态</div><h1>{{ selectedNav === 'items' ? '物品清单' : '物品总览' }}</h1><p class="page-subtitle">{{ selectedNav === 'items' ? '把每件物品的状态和到期时间都管理好。' : `今天是 ${new Date().getMonth()+1} 月 ${new Date().getDate()} 日 · 为新鲜留一点心思。` }}</p></div>
          <div class="welcome-actions"><button class="secondary-button" @click="showNotifications=!showNotifications"><span>◷</span>提醒中心<span v-if="unreadNotifications" class="button-count">{{ unreadNotifications }}</span></button><button class="primary-button" @click="openAdd"><span class="plus">＋</span>添加物品</button></div>
        </div>

        <div class="stats-grid">
          <article class="stat-card stat-total"><div class="stat-top"><span class="stat-label">全部物品</span><span class="stat-icon icon-indigo">▤</span></div><div class="stat-value">{{ activeItems.length }}<small>件</small></div><div class="stat-foot"><span class="stat-mark">↗</span> 正在管理的物品</div><div class="stat-sparkline"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></article>
          <article class="stat-card stat-soon"><div class="stat-top"><span class="stat-label">7 天内到期</span><span class="stat-icon icon-amber">◷</span></div><div class="stat-value">{{ expiringCount }}<small>件</small></div><div class="stat-foot"><span class="status-pill warning-pill">需要关注</span></div><div class="stat-sparkline amber-spark"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></article>
          <article class="stat-card stat-expired"><div class="stat-top"><span class="stat-label">已过期</span><span class="stat-icon icon-rose">!</span></div><div class="stat-value">{{ expiredCount }}<small>件</small></div><div class="stat-foot"><span :class="expiredCount ? 'status-pill danger-pill':'status-pill calm-pill'">{{ expiredCount ? '建议尽快处理' : '状态良好' }}</span></div><div class="stat-sparkline rose-spark"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></article>
          <article class="freshness-card"><div class="freshness-copy"><span class="freshness-label">本周新鲜度</span><div class="freshness-score">8.6<span>/ 10</span></div><p>管理得不错，继续保持！</p><button @click="selectedNav='items'; activeFilter='normal'">查看状态报告 <span>→</span></button></div><div class="freshness-art"><div class="sun-glow"></div><div class="sun-fruit"><span class="leaf"></span><i>☺</i></div><span class="sparkle sparkle-one">✦</span><span class="sparkle sparkle-two">✧</span></div></article>
        </div>

        <div class="content-grid">
          <section class="panel list-panel">
            <div class="panel-heading"><div><div class="panel-title-row"><h2>{{ selectedNav === 'items' ? '物品清单' : '需要关注' }}</h2><span class="soft-count">{{ selectedNav === 'items' ? filteredItems.length : currentAlerts.length }}</span></div><p>{{ selectedNav === 'items' ? '查看和管理你记录的所有物品' : '这些物品将在近期到期，记得及时处理' }}</p></div><button v-if="selectedNav !== 'items'" class="text-link" @click="selectedNav='items'; activeFilter='all'">查看全部 <span>→</span></button></div>
            <div class="toolbar">
              <div class="filter-tabs"><button :class="{selected:activeFilter==='all'}" @click="setFilter('all')">全部 <span>{{ items.length }}</span></button><button :class="{selected:activeFilter==='soon'}" @click="setFilter('soon')">临期 <span>{{ expiringCount }}</span></button><button :class="{selected:activeFilter==='expired'}" @click="setFilter('expired')">已过期</button></div>
              <div class="toolbar-controls"><select v-model="categoryFilter" aria-label="分类筛选"><option v-for="cat in categoryOptions" :key="cat">{{ cat }}</option></select><button class="sort-button" @click="toggleSort">到期日期 <span>{{ sortDirection==='asc' ? '↑' : '↓' }}</span></button></div>
            </div>
            <div class="table-wrap">
              <table class="items-table">
                <thead><tr><th class="check-col"><input type="checkbox" aria-label="全选" /></th><th>物品名称</th><th>分类 / 位置</th><th>购买日期</th><th>到期日期</th><th>状态</th><th></th></tr></thead>
                <tbody>
                  <tr v-if="loading"><td colspan="7" class="empty-state">正在加载物品…</td></tr>
                  <tr v-else-if="!filteredItems.length"><td colspan="7" class="empty-state"><div class="empty-illustration">⌕</div><strong>没有找到物品</strong><span>换个关键词或调整筛选条件试试</span><button @click="search=''; categoryFilter='全部分类'; activeFilter='all'">清除筛选</button></td></tr>
                  <tr v-for="item in filteredItems" :key="item.id" class="item-row">
                    <td class="check-col"><input type="checkbox" :aria-label="`选择${item.name}`" /></td>
                    <td><div class="item-name-cell"><span :class="['item-emoji', `emoji-${item.color || 'green'}`]">{{ item.emoji || '📦' }}</span><span class="item-name-stack"><strong>{{ displayName(item) }}</strong><small>{{ item.quantity || 1 }} {{ item.unit || '件' }} <i v-if="item.notes">· {{ item.notes }}</i></small></span></div></td>
                    <td><span class="category-name">{{ item.category || '其他' }}</span><small class="location-name">{{ item.location || '未设置位置' }}</small></td>
                    <td class="date-cell">{{ dateLabel(item.purchasedOn || item.purchased_on) }}</td>
                    <td><strong class="expiry-date">{{ dateLabel(item.expiresOn || item.expires_on) }}</strong><small class="year-label">{{ (item.expiresOn || item.expires_on || '').slice(0,4) }}</small></td>
                    <td><span :class="['status-badge', `badge-${itemState(item)}`]"><i></i>{{ stateLabel(item) }}</span></td>
                    <td><div class="row-actions"><button title="编辑物品" @click="openEdit(item)">✎</button><button title="标记已处理" @click="markDone(item)">✓</button><button title="删除物品" @click="deleteItem(item)">×</button></div></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="table-footer"><span>显示 {{ filteredItems.length }} 件物品</span><div class="pagination"><button disabled>‹</button><button class="page-active">1</button><button>2</button><button>3</button><button>›</button></div></div>
          </section>

          <aside class="right-column">
            <section class="panel week-panel"><div class="side-panel-heading"><div><h2>本周到期</h2><p>提前安排，减少浪费</p></div><button class="more-button" @click="showExpiryList">···</button></div><div class="week-summary"><div class="week-number">{{ expiringCount }}<small>件</small></div><div class="week-summary-copy"><strong>需要你的关注</strong><span>按到期时间排序</span></div><span class="week-arrow">↗</span></div><div class="mini-timeline"><div v-for="item in upcomingItems.slice(0,4)" :key="item.id" class="timeline-item"><div class="timeline-dot"></div><div class="timeline-content"><div><strong>{{ displayName(item) }}</strong><span>{{ stateLabel(item) }}</span></div><small>{{ dateLabel(item.expiresOn || item.expires_on) }} · {{ item.location || '未设置位置' }}</small></div><span class="timeline-emoji">{{ item.emoji || '📦' }}</span></div><div v-if="!upcomingItems.length" class="timeline-empty">这周没有即将到期的物品 ✨</div></div><button class="full-width-link" @click="showExpiryList">查看临期清单 <span>→</span></button></section>
            <section class="reminder-card"><div class="reminder-decoration">◌</div><span class="reminder-kicker">小提醒</span><h3>让新鲜<br />刚刚好。</h3><p>定期看看你的物品清单，减少过期和浪费。</p><div class="reminder-bottom"><span>一起养成好习惯</span><span>✦</span></div></section>
          </aside>
        </div>
        <div v-if="errorMessage && !showModal" class="toast-error">{{ errorMessage }} <button @click="errorMessage=''">×</button></div>
        <footer class="page-footer"><span>ExpiryMate</span><span>把新鲜留在每一天 <i>✦</i></span><span>{{ isApiMode ? 'API 已配置' : '本地演示模式' }}</span></footer>
      </section>
    </main>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal=false">
      <section class="item-modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <header class="modal-header"><div><span class="modal-kicker">{{ editingId ? '更新物品信息' : '记录新物品' }}</span><h2 id="modal-title">{{ editingId ? '编辑物品' : '添加到你的物品清单' }}</h2><p>包装上的信息可以稍后补充。</p></div><button class="modal-close" aria-label="关闭" @click="showModal=false">×</button></header>
        <form @submit.prevent="saveItem">
          <div class="form-grid"><label class="form-field span-two"><span>物品名称 <b>*</b></span><input v-model="form.name" autofocus placeholder="例如：鲜牛奶" /></label>
            <label class="form-field"><span>分类</span><select v-model="form.category"><option>食品</option><option>个护</option><option>保健品</option><option>清洁</option><option>其他</option></select></label>
            <label class="form-field"><span>存放位置</span><input v-model="form.location" placeholder="例如：冷藏室" /></label>
            <label class="form-field"><span>生产日期</span><input v-model="form.manufacturedOn" type="date" /></label>
            <label class="form-field"><span>购买日期</span><input v-model="form.purchasedOn" type="date" /></label>
            <div class="form-field"><span>保质期</span><div class="inline-input"><input v-model="form.shelfLifeValue" type="number" min="1" placeholder="填写时长" /><select v-model="form.shelfLifeUnit"><option>天</option><option>月</option><option>年</option></select></div></div>
            <label class="form-field"><span>包装到期日</span><input v-model="form.expiresOn" type="date" /><small>填写后以包装日期为准</small></label>
            <div class="form-field"><span>数量</span><div class="inline-input"><input v-model="form.quantity" type="number" min="1" /><select v-model="form.unit"><option>件</option><option>瓶</option><option>盒</option><option>袋</option><option>个</option><option>支</option></select></div></div>
            <label class="form-field span-two"><span>存放位置备注</span><textarea v-model="form.notes" rows="2" placeholder="可选，补充品牌、开封情况等"></textarea></label>
          </div>
          <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
          <div class="modal-footer"><button type="button" class="secondary-button" @click="showModal=false">取消</button><button type="submit" class="primary-button" :disabled="saving">{{ saving ? '保存中…' : editingId ? '保存修改' : '保存物品' }}</button></div>
        </form>
      </section>
    </div>
  </div>
</template>
