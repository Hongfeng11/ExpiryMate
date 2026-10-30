import { apiRequest } from './api'

export const getLists = () => apiRequest('/api/v1/lists')
export const createList = name => apiRequest('/api/v1/lists', { method: 'POST', body: JSON.stringify({ name }) })
export const createInvitation = (listId, role = 'EDITOR') => apiRequest(`/api/v1/lists/${encodeURIComponent(listId)}/invitations`, {
  method: 'POST', body: JSON.stringify({ role }),
})
export const acceptInvitation = token => apiRequest('/api/v1/invitations/accept', {
  method: 'POST', body: JSON.stringify({ token }),
})
