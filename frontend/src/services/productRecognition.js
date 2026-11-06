import { apiRequest } from './api'

export function recognizeProductImage(imageDataUrl) {
  return apiRequest('/api/v1/ai/recognize', {
    method: 'POST',
    body: JSON.stringify({ imageDataUrl }),
  })
}
