const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const STORAGE_KEY = 'expirymate.items.v1'

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
async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    credentials: 'include',
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `请求失败 (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}

export const isApiMode = Boolean(API_BASE)
export async function listItems() {
  if (isApiMode) {
    const data = await request('/api/v1/items?page=1&page_size=100&sort_by=expires_on&sort_order=asc')
    return data.items || data.data?.items || data.data || []
  }
  return localItems()
}
export async function createItem(item) {
  if (isApiMode) return request('/api/v1/items', { method: 'POST', body: JSON.stringify(item) })
  const items = localItems()
  const created = { ...item, id: crypto.randomUUID(), status: 'active' }
  items.unshift(created)
  save(items)
  return created
}
export async function removeItem(id) {
  if (isApiMode) return request(`/api/v1/items/${id}`, { method: 'DELETE' })
  save(localItems().filter(item => item.id !== id))
}
export async function updateItem(id, changes) {
  if (isApiMode) return request(`/api/v1/items/${id}`, { method: 'PATCH', body: JSON.stringify(changes) })
  const items = localItems().map(item => item.id === id ? { ...item, ...changes } : item)
  save(items)
}
