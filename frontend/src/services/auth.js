import { API_BASE, apiRequest } from './api'

export const isApiMode = Boolean(API_BASE)

export async function currentUser() {
  try { return await apiRequest('/api/v1/auth/me') }
  catch (error) { if (error.message?.includes('(401)')) return null; throw error }
}
export function signIn(credentials) {
  return apiRequest('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(credentials) })
}
export function signUp(details) {
  return apiRequest('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(details) })
}
export function signOut() {
  return apiRequest('/api/v1/auth/logout', { method: 'POST' })
}
