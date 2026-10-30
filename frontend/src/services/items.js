import { API_BASE, apiRequest } from './api'
const STORAGE_KEY = 'expirymate.items.v1'
let activeListId = ''
export const isApiMode = Boolean(API_BASE)
export function setActiveListId(id) { activeListId = id || '' }
function listPath() {
  if (!activeListId) throw new Error('请先选择一个物品清单')
  return `/api/v1/lists/${encodeURIComponent(activeListId)}/items`
}

function dateOffset(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function starterItems() {
  return [
    { id: 'demo-1', name: '鲜牛奶', category: '食品', location: '冷藏室', quantity: 1, unit: '瓶', purchasedOn: dateOffset(-5), manufacturedOn: dateOffset(-8), expiresOn: dateOffset(2), emoji: '🥛', color: 'amber', status: 'active' },
    { id: 'demo-2', name: '新鲜草莓', category: '食品', location: '冷藏室', quantity: 1, unit: '盒', purchasedOn: dateOffset(-3), manufacturedOn: dateOffset(-4), expiresOn: dateOffset(3), emoji: '🍓', color: 'rose', status: 'active' },
    { id: 'demo-3', name: '清爽防晒乳', category: '个护', location: '浴室柜', quantity: 1, unit: '支', purchasedOn: dateOffset(-40), manufacturedOn: dateOffset(-120), expiresOn: dateOffset(92), emoji: '🧴', color: 'blue', status: 'active' },
    { id: 'demo-4', name: '维生素片', category: '保健品', location: '书桌抽屉', quantity: 1, unit: '瓶', purchasedOn: dateOffset(-25), manufacturedOn: dateOffset(-100), expiresOn: dateOffset(164), emoji: '💊', color: 'violet', status: 'active' },
    { id: 'demo-5', name: '希腊酸奶', category: '食品', location: '冷藏室', quantity: 2, unit: '杯', purchasedOn: dateOffset(-8), manufacturedOn: dateOffset(-10), expiresOn: dateOffset(-1), emoji: '🥣', color: 'pink', status: 'active' },
  ]
}

function localItems() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) return JSON.parse(saved)
  const initial = starterItems()
  localStorage.setItem(STORAGE_KEY, JSON.stringify(initial))
  return initial
}
function save(items) { localStorage.setItem(STORAGE_KEY, JSON.stringify(items)) }
function toApiItem(item) {
  const units = { 天: 'days', 月: 'months', 年: 'years' }
  const hasShelfLife = item.shelfLifeValue !== '' && item.shelfLifeValue != null
  const hasOpenedShelfLife = item.openedShelfLifeValue !== '' && item.openedShelfLifeValue != null
  const result = {
    ...item,
    shelfLifeValue: hasShelfLife ? Number(item.shelfLifeValue) : null,
    shelfLifeUnit: hasShelfLife ? (units[item.shelfLifeUnit] || item.shelfLifeUnit) : null,
    openedShelfLifeValue: hasOpenedShelfLife ? Number(item.openedShelfLifeValue) : null,
    openedShelfLifeUnit: hasOpenedShelfLife ? (units[item.openedShelfLifeUnit] || item.openedShelfLifeUnit) : null,
    status: item.status === 'done' ? 'processed' : item.status,
  }
  if (Object.hasOwn(item, 'packageExpiresOn')) result.clearPackageExpiresOn = !item.packageExpiresOn
  if (Object.hasOwn(item, 'openedOn')) result.clearOpenedOn = !item.openedOn
  if (Object.hasOwn(item, 'shelfLifeValue')) result.clearShelfLife = !hasShelfLife
  if (Object.hasOwn(item, 'openedShelfLifeValue')) result.clearOpenedShelfLife = !hasOpenedShelfLife
  return result
}
export async function listItems(filters = {}) {
  if (isApiMode) {
    const query = new URLSearchParams({ page: '1', page_size: '100', sort_by: 'expires_on', sort_order: 'asc' })
    if (filters.category && filters.category !== '全部分类') query.set('category', filters.category)
    if (filters.location && filters.location !== '全部位置') query.set('location', filters.location)
    if (filters.q?.trim()) query.set('q', filters.q.trim())
    const data = await apiRequest(`${listPath()}?${query.toString()}`)
    return data.items || data.data?.items || data.data || []
  }
  const items = localItems()
  const q = filters.q?.trim().toLowerCase()
  return items.filter(item =>
    (!filters.category || filters.category === '全部分类' || item.category === filters.category)
    && (!filters.location || filters.location === '全部位置' || item.location === filters.location)
    && (!q || [item.name, item.category, item.location, item.notes].some(value => (value || '').toLowerCase().includes(q)))
  )
}
export async function createItem(item) {
  if (isApiMode) return apiRequest(listPath(), { method: 'POST', body: JSON.stringify(toApiItem(item)) })
  const items = localItems()
  const created = { ...item, id: crypto.randomUUID(), status: 'active' }
  items.unshift(created)
  save(items)
  return created
}
export async function removeItem(id) {
  if (isApiMode) return apiRequest(`${listPath()}/${encodeURIComponent(id)}`, { method: 'DELETE' })
  save(localItems().filter(item => item.id !== id))
}
export async function updateItem(id, changes) {
  if (isApiMode) return apiRequest(`${listPath()}/${encodeURIComponent(id)}`, { method: 'PATCH', body: JSON.stringify(toApiItem(changes)) })
  const items = localItems().map(item => item.id === id ? { ...item, ...changes } : item)
  save(items)
}
