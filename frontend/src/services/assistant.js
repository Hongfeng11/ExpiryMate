import { loadRecognitionSettings } from './productRecognition'

export async function askFoodAssistant(messages, foodItems) {
  const { endpoint, apiKey, model } = loadRecognitionSettings()
  if (!endpoint || !model) throw new Error('请先配置识图 / AI API 地址和模型名称')

  const today = new Date().toISOString().slice(0, 10)
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}),
    },
    body: JSON.stringify({
      model,
      temperature: 0.4,
      messages: [
        {
          role: 'system',
          content: `你是鲜期管家的家庭饮食助手。今天日期是 ${today}。请只根据用户清单中的食品给出临期优先顺序和菜谱建议。过期食品应标记为“已过期，不建议食用”，不要把它们放进菜谱；先处理未过期且最早到期的食品。不要臆造清单中没有的保质期、食材或用户过敏信息。日期缺失时明确说明。建议简洁、实用，菜谱包含食材和简要步骤。若用户没有说明口味或过敏情况，提醒其自行核对配料。清单数据(JSON)：${JSON.stringify(foodItems)}`,
        },
        ...messages,
      ],
    }),
  }).catch(error => {
    if (error instanceof TypeError) throw new Error('无法连接 AI API，请检查地址、网络和服务商的 CORS 设置')
    throw error
  })

  const payload = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(payload.error?.message || payload.message || `AI 请求失败（${response.status}）`)
  const content = payload.choices?.[0]?.message?.content
  const answer = Array.isArray(content) ? content.map(part => part.text || '').join('\n') : content
  if (!answer) throw new Error('AI API 没有返回文字内容')
  return String(answer).trim()
}
