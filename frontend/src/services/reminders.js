const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const STORAGE_KEY = 'expirymate.reminders.v1'
const DEFAULT_SETTINGS = { enabled: true, remindBeforeDays: [7, 3, 1, 0], preferredTime: '09:00' }

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
  return response.json()
}

export async function getReminderSettings() {
  if (API_BASE) {
    const settings = await request('/api/v1/settings/reminders')
    return { ...settings, preferredTime: (settings.preferredTime || '09:00').slice(0, 5) }
  }
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved ? { ...DEFAULT_SETTINGS, ...JSON.parse(saved) } : { ...DEFAULT_SETTINGS }
}

export async function saveReminderSettings(settings) {
  const normalized = {
    enabled: Boolean(settings.enabled),
    remindBeforeDays: [...new Set(settings.remindBeforeDays.map(Number))].sort((a, b) => b - a),
    preferredTime: settings.preferredTime || '09:00',
  }
  if (API_BASE) {
    const saved = await request('/api/v1/settings/reminders', { method: 'PUT', body: JSON.stringify(normalized) })
    return { ...saved, preferredTime: (saved.preferredTime || '09:00').slice(0, 5) }
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}
