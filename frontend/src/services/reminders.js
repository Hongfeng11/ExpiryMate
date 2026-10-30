import { API_BASE, apiRequest } from './api'
const STORAGE_KEY = 'expirymate.reminders.v1'
const DEFAULT_SETTINGS = { enabled: true, remindBeforeDays: [7, 3, 1, 0], preferredTime: '09:00' }

export async function getReminderSettings() {
  if (API_BASE) {
    const settings = await apiRequest('/api/v1/settings/reminders')
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
    const saved = await apiRequest('/api/v1/settings/reminders', { method: 'PUT', body: JSON.stringify(normalized) })
    return { ...saved, preferredTime: (saved.preferredTime || '09:00').slice(0, 5) }
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}
