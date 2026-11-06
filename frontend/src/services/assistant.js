import { apiRequest } from './api'

export async function askFoodAssistant(messages, foodItems) {
  const payload = await apiRequest('/api/v1/ai/chat', {
    method: 'POST',
    body: JSON.stringify({ messages, foods: foodItems }),
  })
  const content = payload.choices?.[0]?.message?.content
  const answer = Array.isArray(content) ? content.map(part => part.text || '').join('\n') : content
  if (!answer) throw new Error('AI 服务没有返回文字内容')
  return String(answer).trim()
}
