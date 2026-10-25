<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createItem, isApiMode, listItems, removeItem, updateItem } from './services/items'
import { getReminderSettings, saveReminderSettings } from './services/reminders'

const items = ref([])
const selectedNav = ref('overview')
const activeFilter = ref('all')
const search = ref('')
const categoryFilter = ref('全部分类')
const locationFilter = ref('全部位置')
const catalogItems = ref([])
const showModal = ref(false)
const showNotifications = ref(false)
const showReminderSettings = ref(false)
const loading = ref(true)
const errorMessage = ref('')
const sortDirection = ref('asc')
const notificationRead = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref(emptyForm())
const reminderSettings = ref({ enabled: true, remindBeforeDays: [7, 3, 1, 0], preferredTime: '09:00' })
const reminderDraft = ref({ enabled: true, remindBeforeDays: [7, 3, 1, 0], preferredTime: '09:00' })
const reminderSaving = ref(false)
const reminderError = ref('')
const reminderSaved = ref(false)
const expiryPreview = computed(() => {
  const baseDate = calculatedBaseExpiry()
  const openedDate = calculatedOpenedExpiry()
  if (!baseDate && !openedDate) return null
  if (!baseDate) return { date: openedDate, source: 'openedOnly' }
  if (openedDate && openedDate < baseDate) return { date: openedDate, source: 'opened' }
  return { date: baseDate, source: baseDate === form.value.packageExpiresOn ? 'package' : 'calculated' }
})

function emptyForm() {
  return { name: '', category: '食品', location: '', quantity: 1, unit: '件', manufacturedOn: '', purchasedOn: '', shelfLifeValue: '', shelfLifeUnit: '天', packageExpiresOn: '', openedOn: '', openedShelfLifeValue: '', openedShelfLifeUnit: '天', notes: '' }
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
  if (['done', 'used_up', 'discarded', 'processed'].includes(String(item.status || '').toLowerCase())) return 'done'
  const days = daysLeft(item.expiresOn || item.expires_on)
  if (days < 0) return 'expired'
  if (days <= reminderWindow.value) return 'soon'
  return 'normal'
}
function stateLabel(item) {
  const state = itemState(item)
  if (state === 'done') {
    const labels = { used_up: '已用完', discarded: '已丢弃', processed: '已处理', done: '已处理' }
    return labels[String(item.status || '').toLowerCase()] || '已处理'
  }
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
const activeItems = computed(() => catalogItems.value.filter(item => item.status === 'active' || !item.status))
const reminderWindow = computed(() => Math.max(0, ...reminderSettings.value.remindBeforeDays))
const expiringCount = computed(() => activeItems.value.filter(item => itemState(item) === 'soon').length)
const expiredCount = computed(() => activeItems.value.filter(item => itemState(item) === 'expired').length)
const currentAlerts = computed(() => reminderSettings.value.enabled ? activeItems.value.filter(item => {
  const days = daysLeft(item.expiresOn || item.expires_on)
  return days < 0 || reminderSettings.value.remindBeforeDays.includes(days)
}).sort((a, b) => daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)) : [])
const upcomingItems = computed(() => activeItems.value.filter(item => itemState(item) === 'soon').sort((a, b) => daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)))
const nextSevenDays = computed(() => Array.from({ length: 7 }, (_, offset) => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + offset)
  const key = dateString(date)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return {
    key,
    offset,
    dayLabel: offset === 0 ? '今' : `周${weekdays[date.getDay()]}`,
    dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
    count: activeItems.value.filter(item => (item.expiresOn || item.expires_on) === key).length,
  }
}))
const nextSevenDaysCount = computed(() => nextSevenDays.value.reduce((sum, day) => sum + day.count, 0))
const nextSevenDaysMax = computed(() => Math.max(1, ...nextSevenDays.value.map(day => day.count)))
const filteredItems = computed(() => {
  const q = search.value.trim().toLowerCase()
  return items.value.filter(item => {
    const state = itemState(item)
    const statusOk = activeFilter.value === 'all' || (activeFilter.value === 'soon' && state === 'soon') || (activeFilter.value === 'expired' && state === 'expired') || (activeFilter.value === 'normal' && state === 'normal') || (activeFilter.value === 'done' && state === 'done')
    const categoryOk = categoryFilter.value === '全部分类' || item.category === categoryFilter.value
    const locationOk = locationFilter.value === '全部位置' || item.location === locationFilter.value
    const queryOk = !q || [item.name, item.category, item.location, item.notes].some(value => (value || '').toLowerCase().includes(q))
    return statusOk && categoryOk && locationOk && queryOk
  }).sort((a, b) => (daysLeft(a.expiresOn || a.expires_on) - daysLeft(b.expiresOn || b.expires_on)) * (sortDirection.value === 'asc' ? 1 : -1))
})
const categoryOptions = computed(() => ['全部分类', ...new Set(catalogItems.value.map(item => item.category).filter(Boolean))])
const locationOptions = computed(() => ['全部位置', ...new Set(catalogItems.value.map(item => item.location).filter(Boolean))])
const unreadNotifications = computed(() => currentAlerts.value.length)

let itemRequestVersion = 0
let catalogLoaded = false
async function refresh({ reloadCatalog = false } = {}) {
  const requestVersion = ++itemRequestVersion
  loading.value = true
  errorMessage.value = ''
  try {
    if (reloadCatalog || !catalogLoaded) {
      const allItems = await listItems()
      if (requestVersion !== itemRequestVersion) return
      catalogItems.value = allItems
      catalogLoaded = true
      if (!isApiMode) { items.value = allItems; return }
    }
    if (requestVersion !== itemRequestVersion) return
    items.value = isApiMode
      ? await listItems({ q: search.value, category: categoryFilter.value, location: locationFilter.value })
      : catalogItems.value
  } catch (error) {
    if (requestVersion === itemRequestVersion) { errorMessage.value = error.message || '暂时无法连接服务'; items.value = [] }
  } finally {
    if (requestVersion === itemRequestVersion) loading.value = false
  }
}
watch([search, categoryFilter, locationFilter], (_values, _oldValues, onCleanup) => {
  if (!isApiMode || !catalogLoaded) return
  const timer = window.setTimeout(() => refresh(), 250)
  onCleanup(() => window.clearTimeout(timer))
})
async function openReminderSettings() {
  reminderError.value = ''
  reminderSaved.value = false
  try {
    reminderSettings.value = await getReminderSettings()
    reminderDraft.value = { ...reminderSettings.value, remindBeforeDays: [...reminderSettings.value.remindBeforeDays] }
    showReminderSettings.value = true
  } catch (error) { errorMessage.value = error.message || '无法读取提醒设置' }
}
function toggleReminderDay(day) {
  const days = reminderDraft.value.remindBeforeDays
  reminderDraft.value.remindBeforeDays = days.includes(day) ? days.filter(value => value !== day) : [...days, day].sort((a, b) => b - a)
}
async function persistReminderSettings() {
  reminderError.value = ''
  reminderSaved.value = false
  if (!reminderDraft.value.remindBeforeDays.length) { reminderError.value = '至少选择一个提前提醒时间'; return }
  reminderSaving.value = true
  try {
    reminderSettings.value = await saveReminderSettings(reminderDraft.value)
    reminderDraft.value = { ...reminderSettings.value, remindBeforeDays: [...reminderSettings.value.remindBeforeDays] }
    reminderSaved.value = true
  } catch (error) { reminderError.value = error.message || '保存提醒设置失败' }
  finally { reminderSaving.value = false }
}
function openAdd() { editingId.value = null; form.value = emptyForm(); errorMessage.value = ''; showModal.value = true }
function openEdit(item) {
  editingId.value = item.id
  const hasStoredShelfLife = item.shelfLifeValue || item.shelf_life_value
  const hasStoredOpenedShelfLife = item.openedShelfLifeValue || item.opened_shelf_life_value
  const packageExpiry = item.packageExpiresOn || item.package_expires_on || ((!hasStoredShelfLife && !hasStoredOpenedShelfLife) ? item.expiresOn || item.expires_on || '' : '')
  form.value = { ...emptyForm(), ...item, manufacturedOn: item.manufacturedOn || item.manufactured_on || '', purchasedOn: item.purchasedOn || item.purchased_on || '', packageExpiresOn: packageExpiry, openedOn: item.openedOn || item.opened_on || '', shelfLifeUnit: shelfLifeUnitLabel(item.shelfLifeUnit || item.shelf_life_unit) || emptyForm().shelfLifeUnit, openedShelfLifeUnit: shelfLifeUnitLabel(item.openedShelfLifeUnit || item.opened_shelf_life_unit) || emptyForm().openedShelfLifeUnit }
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
function shelfLifeUnitLabel(unit) { return ({ days: '天', months: '月', years: '年', '天': '天', '月': '月', '年': '年' })[unit] || '' }
function addShelfLife(dateValue, value, rawUnit) {
  if (!dateValue || !value || !Number.isInteger(Number(value)) || Number(value) < 1) return ''
  const base = parseDate(dateValue)
  const amount = Number(value)
  const unit = shelfLifeUnitLabel(rawUnit)
  if (unit === '天') base.setDate(base.getDate() + amount)
  if (unit === '月') return dateString(addMonths(base, amount))
  if (unit === '年') return dateString(addMonths(base, amount * 12))
  return unit === '天' ? dateString(base) : ''
}
function calculatedBaseExpiry() {
  if (form.value.packageExpiresOn) return form.value.packageExpiresOn
  return addShelfLife(form.value.manufacturedOn, form.value.shelfLifeValue, form.value.shelfLifeUnit)
}
function calculatedOpenedExpiry() {
  return addShelfLife(form.value.openedOn, form.value.openedShelfLifeValue, form.value.openedShelfLifeUnit)
}
function calculatedExpiry() {
  return [calculatedBaseExpiry(), calculatedOpenedExpiry()].filter(Boolean).sort()[0] || ''
}
async function saveItem() {
  errorMessage.value = ''
  if (!form.value.name.trim()) { errorMessage.value = '请填写物品名称'; return }
  const hasOpenedShelfLife = form.value.openedShelfLifeValue !== '' && form.value.openedShelfLifeValue != null
  if (hasOpenedShelfLife && !form.value.openedOn) { errorMessage.value = '填写开封后期限时，请同时填写开封日期'; return }
  const expiresOn = calculatedExpiry()
  if (!expiresOn) { errorMessage.value = '请填写包装到期日，或填写生产日期和保质期；也可填写开封日期和开封后期限'; return }
  saving.value = true
  try {
    const payload = { ...form.value, name: form.value.name.trim(), expiresOn, quantity: Number(form.value.quantity) || 1, emoji: emojiFor(form.value.category), color: colorFor(form.value.category), status: 'active' }
    if (editingId.value) await updateItem(editingId.value, payload)
    else await createItem(payload)
    showModal.value = false
    await refresh({ reloadCatalog: true })
  } catch (error) { errorMessage.value = error.message || '保存失败，请稍后重试' }
  finally { saving.value = false }
}
function emojiFor(category) { return ({ 食品: '🥬', 个护: '🧴', 保健品: '💊', 清洁: '🧽' })[category] || '📦' }
function colorFor(category) { return ({ 食品: 'green', 个护: 'blue', 保健品: 'violet', 清洁: 'cyan' })[category] || 'amber' }
async function setItemStatus(item, status) {
  try {
    const quantity = status === 'active' && Number(item.quantity) <= 0 ? 1
      : ['used_up', 'discarded'].includes(status) ? 0
        : Number(item.quantity)
    await updateItem(item.id, { status, quantity })
    await refresh({ reloadCatalog: true })
  } catch (error) { errorMessage.value = error.message || '更新库存状态失败' }
}
async function adjustQuantity(item, delta) {
  if (item.status && item.status !== 'active') return
  const current = Number(item.quantity) || 0
  const quantity = Math.max(0, Number((current + delta).toFixed(2)))
  const status = quantity === 0 ? 'used_up' : 'active'
  try {
    await updateItem(item.id, { quantity, status })
    await refresh({ reloadCatalog: true })
  } catch (error) { errorMessage.value = error.message || '更新库存数量失败' }
}
async function deleteItem(item) {
  if (!window.confirm(`确定删除「${item.name}」吗？`)) return
  try { await removeItem(item.id); await refresh({ reloadCatalog: true }) }
  catch (error) { errorMessage.value = error.message || '删除失败' }
}
function toggleSort() { sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc' }
function setFilter(filter) { activeFilter.value = filter; selectedNav.value = filter === 'all' ? 'overview' : 'items' }
function showExpiryList() { clearFilters(); selectedNav.value = 'items'; activeFilter.value = 'soon' }
function clearFilters() { search.value = ''; categoryFilter.value = '全部分类'; locationFilter.value = '全部位置'; activeFilter.value = 'all' }
function openOverviewItems() { clearFilters(); selectedNav.value = 'items' }
function weekBarHeight(day) { return day.count ? `${Math.max(12, (day.count / nextSevenDaysMax.value) * 100)}%` : '4%' }
function displayName(item) { return item.name || '未命名物品' }
onMounted(async () => {
  try { reminderSettings.value = await getReminderSettings() }
  catch (error) { errorMessage.value = error.message || '无法读取提醒设置' }
  await refresh()
})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">◈</span><span>ExpiryMate<small>物品保质期管理</small></span></div>
      <div class="workspace-label">工作空间</div>
      <nav class="nav-list">
        <button :class="['nav-link', selectedNav === 'overview' ? 'active' : '']" @click="selectedNav='overview'; activeFilter='all'"><span class="nav-icon">▦</span>总览</button>
        <button :class="['nav-link', selectedNav === 'items' ? 'active' : '']" @click="selectedNav='items'"><span class="nav-icon">▤</span>物品清单<span class="nav-count">{{ catalogItems.length }}</span></button>
        <button class="nav-link" @click="showNotifications=!showNotifications"><span class="nav-icon">◷</span>临期提醒<span v-if="unreadNotifications" class="nav-dot"></span></button>
      </nav>
      <div class="sidebar-divider"></div>
      <div class="workspace-label">快捷分类</div>
      <button class="category-link" @click="categoryFilter='食品'; selectedNav='items'"><i class="category-dot food"></i>食品 <span>{{ items.filter(i=>i.category==='食品').length }}</span></button>
      <button class="category-link" @click="categoryFilter='个护'; selectedNav='items'"><i class="category-dot personal"></i>个护 <span>{{ items.filter(i=>i.category==='个护').length }}</span></button>
      <button class="category-link" @click="categoryFilter='保健品'; selectedNav='items'"><i class="category-dot health"></i>保健品 <span>{{ items.filter(i=>i.category==='保健品').length }}</span></button>
      <div class="sidebar-bottom">
        <div class="tip-card"><div class="tip-icon">✦</div><strong>少一点浪费</strong><p>让每一件物品都在新鲜时被好好使用。</p></div>
        <div class="profile"><div class="avatar">林</div><div><strong>我的家庭</strong><small>个人空间</small></div><button aria-label="提醒设置" title="提醒设置" @click="openReminderSettings">⚙</button></div>
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
          <article class="stat-card stat-soon"><div class="stat-top"><span class="stat-label">{{ reminderWindow }} 天内到期</span><span class="stat-icon icon-amber">◷</span></div><div class="stat-value">{{ expiringCount }}<small>件</small></div><div class="stat-foot"><span class="status-pill warning-pill">{{ reminderSettings.enabled ? '需要关注' : '提醒已关闭' }}</span></div><div class="stat-sparkline amber-spark"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></article>
          <article class="stat-card stat-expired"><div class="stat-top"><span class="stat-label">已过期</span><span class="stat-icon icon-rose">!</span></div><div class="stat-value">{{ expiredCount }}<small>件</small></div><div class="stat-foot"><span :class="expiredCount ? 'status-pill danger-pill':'status-pill calm-pill'">{{ expiredCount ? '建议尽快处理' : '状态良好' }}</span></div><div class="stat-sparkline rose-spark"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></article>
          <article class="expiry-week-card"><div class="expiry-week-heading"><div><span class="freshness-label">未来 7 天到期趋势</span><strong>{{ nextSevenDaysCount }}<small>件</small></strong></div><button @click="openOverviewItems">查看清单 <span>→</span></button></div><div class="expiry-week-chart"><div v-for="day in nextSevenDays" :key="day.key" class="expiry-day"><span class="expiry-day-count">{{ day.count || '' }}</span><div class="expiry-bar-track"><i :class="{ 'has-expiry': day.count }" :style="{ height: weekBarHeight(day) }"></i></div><span class="expiry-day-label">{{ day.dayLabel }}</span><small>{{ day.dateLabel }}</small></div></div><p class="expiry-week-note">按本地日期统计仍在管理中的物品</p></article>
        </div>

        <div class="content-grid">
          <section class="panel list-panel">
            <div class="panel-heading"><div><div class="panel-title-row"><h2>{{ selectedNav === 'items' ? '物品清单' : '需要关注' }}</h2><span class="soft-count">{{ selectedNav === 'items' ? filteredItems.length : currentAlerts.length }}</span></div><p>{{ selectedNav === 'items' ? '查看和管理你记录的所有物品' : '这些物品将在近期到期，记得及时处理' }}</p></div><button v-if="selectedNav !== 'items'" class="text-link" @click="selectedNav='items'; activeFilter='all'">查看全部 <span>→</span></button></div>
            <div class="toolbar">
              <div class="filter-tabs"><button :class="{selected:activeFilter==='all'}" @click="setFilter('all')">全部 <span>{{ catalogItems.length }}</span></button><button :class="{selected:activeFilter==='soon'}" @click="setFilter('soon')">临期 <span>{{ expiringCount }}</span></button><button :class="{selected:activeFilter==='expired'}" @click="setFilter('expired')">已过期</button><button :class="{selected:activeFilter==='done'}" @click="setFilter('done')">已处理 <span>{{ catalogItems.filter(i=>['used_up','discarded','processed','done'].includes(i.status)).length }}</span></button></div>
              <div class="toolbar-controls"><select v-model="categoryFilter" aria-label="分类筛选"><option v-for="cat in categoryOptions" :key="cat">{{ cat }}</option></select><select v-model="locationFilter" aria-label="存放位置筛选"><option v-for="location in locationOptions" :key="location">{{ location }}</option></select><button class="reset-filters" @click="clearFilters">重置</button><button class="sort-button" @click="toggleSort">到期日期 <span>{{ sortDirection==='asc' ? '↑' : '↓' }}</span></button></div>
            </div>
            <div class="table-wrap">
              <table class="items-table">
                <thead><tr><th class="check-col"><input type="checkbox" aria-label="全选" /></th><th>物品名称</th><th>分类 / 位置</th><th>购买日期</th><th>到期日期</th><th>状态</th><th></th></tr></thead>
                <tbody>
                  <tr v-if="loading"><td colspan="7" class="empty-state">正在加载物品…</td></tr>
                  <tr v-else-if="!filteredItems.length"><td colspan="7" class="empty-state"><div class="empty-illustration">⌕</div><strong>没有找到物品</strong><span>换个关键词或调整筛选条件试试</span><button @click="clearFilters">清除筛选</button></td></tr>
                  <tr v-for="item in filteredItems" :key="item.id" class="item-row">
                    <td class="check-col"><input type="checkbox" :aria-label="`选择${item.name}`" /></td>
                    <td><div class="item-name-cell"><span :class="['item-emoji', `emoji-${item.color || 'green'}`]">{{ item.emoji || '📦' }}</span><span class="item-name-stack"><strong>{{ displayName(item) }}</strong><small class="quantity-control"><button type="button" :disabled="itemState(item)==='done'" :aria-label="`减少${item.name}库存`" @click="adjustQuantity(item,-1)">−</button><b>{{ item.quantity ?? 1 }} {{ item.unit || '件' }}</b><button type="button" :aria-label="`增加${item.name}库存`" @click="adjustQuantity(item,1)">＋</button><i v-if="item.notes">· {{ item.notes }}</i></small></span></div></td>
                    <td><span class="category-name">{{ item.category || '其他' }}</span><small class="location-name">{{ item.location || '未设置位置' }}</small></td>
                    <td class="date-cell">{{ dateLabel(item.purchasedOn || item.purchased_on) }}</td>
                    <td><strong class="expiry-date">{{ dateLabel(item.expiresOn || item.expires_on) }}</strong><small class="year-label">{{ (item.expiresOn || item.expires_on || '').slice(0,4) }}</small></td>
                    <td><span :class="['status-badge', `badge-${itemState(item)}`]"><i></i>{{ stateLabel(item) }}</span></td>
                    <td><div class="row-actions"><button title="编辑物品" @click="openEdit(item)">✎</button><select class="status-select" :value="item.status || 'active'" :aria-label="`${item.name}处理状态`" @change="setItemStatus(item,$event.target.value)"><option value="active">使用中</option><option value="used_up">已用完</option><option value="discarded">已丢弃</option><option value="processed">已处理</option></select><button title="删除物品" @click="deleteItem(item)">×</button></div></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="table-footer"><span>显示 {{ filteredItems.length }} 件物品</span><div class="pagination"><button disabled>‹</button><button class="page-active">1</button><button>2</button><button>3</button><button>›</button></div></div>
          </section>

          <aside class="right-column">
            <section class="panel week-panel"><div class="side-panel-heading"><div><h2>近期到期</h2><p>按提醒设置提前安排，减少浪费</p></div><button class="more-button" title="提醒设置" @click="openReminderSettings">⚙</button></div><div class="week-summary"><div class="week-number">{{ expiringCount }}<small>件</small></div><div class="week-summary-copy"><strong>需要你的关注</strong><span>提前 {{ reminderWindow }} 天查看</span></div><span class="week-arrow">↗</span></div><div class="mini-timeline"><div v-for="item in upcomingItems.slice(0,4)" :key="item.id" class="timeline-item"><div class="timeline-dot"></div><div class="timeline-content"><div><strong>{{ displayName(item) }}</strong><span>{{ stateLabel(item) }}</span></div><small>{{ dateLabel(item.expiresOn || item.expires_on) }} · {{ item.location || '未设置位置' }}</small></div><span class="timeline-emoji">{{ item.emoji || '📦' }}</span></div><div v-if="!upcomingItems.length" class="timeline-empty">这段时间没有即将到期的物品 ✨</div></div><button class="full-width-link" @click="showExpiryList">查看临期清单 <span>→</span></button></section>
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
            <label class="form-field span-two"><span>包装标注到期日 <small>优先填写</small></span><input v-model="form.packageExpiresOn" type="date" /><small v-if="expiryPreview?.source === 'opened'">开封后期限更早，当前到期日为：{{ dateLabel(expiryPreview.date) }}</small><small v-else-if="expiryPreview?.source === 'openedOnly'">根据开封日期和开封后期限计算：{{ dateLabel(expiryPreview.date) }}</small><small v-else-if="expiryPreview?.source === 'package'">将以包装标注日期为基础计算</small><small v-else-if="expiryPreview">未填写包装日期，将自动计算：{{ dateLabel(expiryPreview.date) }}</small><small v-else>没有标注到期日时，请填写生产日期和保质期，或开封日期和开封后期限</small></label>
            <label class="form-field"><span>生产日期</span><input v-model="form.manufacturedOn" type="date" /></label>
            <label class="form-field"><span>购买日期</span><input v-model="form.purchasedOn" type="date" /></label>
            <div class="form-field"><span>保质期</span><div class="inline-input"><input v-model="form.shelfLifeValue" type="number" min="1" step="1" placeholder="填写时长" /><select v-model="form.shelfLifeUnit"><option>天</option><option>月</option><option>年</option></select></div></div>
            <label class="form-field"><span>开封日期</span><input v-model="form.openedOn" type="date" /></label>
            <div class="form-field"><span>开封后期限</span><div class="inline-input"><input v-model="form.openedShelfLifeValue" type="number" min="1" step="1" placeholder="选填时长" /><select v-model="form.openedShelfLifeUnit"><option>天</option><option>月</option><option>年</option></select></div></div>
            <div class="form-field"><span>数量</span><div class="inline-input"><input v-model="form.quantity" type="number" min="1" /><select v-model="form.unit"><option>件</option><option>瓶</option><option>盒</option><option>袋</option><option>个</option><option>支</option></select></div></div>
            <label class="form-field span-two"><span>存放位置备注</span><textarea v-model="form.notes" rows="2" placeholder="可选，补充品牌、开封情况等"></textarea></label>
          </div>
          <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
          <div class="modal-footer"><button type="button" class="secondary-button" @click="showModal=false">取消</button><button type="submit" class="primary-button" :disabled="saving">{{ saving ? '保存中…' : editingId ? '保存修改' : '保存物品' }}</button></div>
        </form>
      </section>
    </div>
    <div v-if="showReminderSettings" class="modal-backdrop" @click.self="showReminderSettings=false">
      <section class="item-modal reminder-settings-modal" role="dialog" aria-modal="true" aria-labelledby="reminder-settings-title">
        <header class="modal-header"><div><span class="modal-kicker">通知偏好</span><h2 id="reminder-settings-title">提醒设置</h2><p>选择提醒提前时间和每日检查时间。</p></div><button class="modal-close" aria-label="关闭" @click="showReminderSettings=false">×</button></header>
        <div class="settings-content">
          <label class="settings-toggle"><span><strong>开启临期提醒</strong><small>关闭后，页面不再显示临期提醒</small></span><input v-model="reminderDraft.enabled" type="checkbox" /></label>
          <div class="settings-group"><strong>提前提醒</strong><small>到期前哪些天显示在提醒中心</small><div class="reminder-day-options"><button v-for="day in [14, 7, 3, 1, 0]" :key="day" type="button" :class="{ selected: reminderDraft.remindBeforeDays.includes(day) }" @click="toggleReminderDay(day)">{{ day === 0 ? '到期当天' : `提前 ${day} 天` }}</button></div></div>
          <label class="settings-group reminder-time"><strong>每日检查时间</strong><small>提醒任务的预设时间</small><input v-model="reminderDraft.preferredTime" type="time" /></label>
          <p class="settings-note">当前版本会在应用内显示提醒。浏览器推送和后台定时发送尚未接入。</p>
          <div v-if="reminderError" class="form-error">{{ reminderError }}</div>
          <div v-if="reminderSaved" class="settings-success">提醒设置已保存</div>
          <div class="modal-footer"><button type="button" class="secondary-button" @click="showReminderSettings=false">关闭</button><button type="button" class="primary-button" :disabled="reminderSaving" @click="persistReminderSettings">{{ reminderSaving ? '保存中…' : '保存设置' }}</button></div>
        </div>
      </section>
    </div>
  </div>
</template>
