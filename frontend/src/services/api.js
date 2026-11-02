const isNativeApp = typeof window !== 'undefined' && Boolean(window.Capacitor?.isNativePlatform?.())
const browserApiBase = typeof window === 'undefined' ? '' : `${window.location.protocol}//${window.location.hostname}:8080`
// Android emulator reaches the development computer through 10.0.2.2.
// Set VITE_API_BASE_URL for physical devices and production builds.
const nativeApiBase = isNativeApp ? 'http://10.0.2.2:8080' : browserApiBase
export const API_BASE = (import.meta.env.VITE_API_BASE_URL || nativeApiBase).replace(/\/$/, '')

export async function apiRequest(path, options = {}) {
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
