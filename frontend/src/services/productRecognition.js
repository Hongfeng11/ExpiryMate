const STORAGE_KEY = 'expirymate.imageRecognition.settings.v1'

export function loadRecognitionSettings() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || { endpoint: '', apiKey: '', model: '' }
  } catch {
    return { endpoint: '', apiKey: '', model: '' }
  }
}

export function saveRecognitionSettings(settings) {
  const value = {
    endpoint: settings.endpoint.trim(),
    apiKey: settings.apiKey.trim(),
    model: settings.model.trim(),
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  return value
}

export async function recognizeProductImage(imageDataUrl, settings) {
  const { endpoint, apiKey, model } = settings
  if (!endpoint || !model) throw new Error('请先填写识图 API 地址和模型名称')

  let response
  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}),
      },
      body: JSON.stringify({
        model,
        temperature: 0.1,
        messages: [{
          role: 'user',
          content: [
            { type: 'text', text: '识别商品包装上的信息，只返回 JSON 对象，不要 Markdown。字段：name(商品名), category(食品/个护/保健品/清洁/其他), manufacturedOn(生产日期，YYYY-MM-DD), packageExpiresOn(包装到期日，YYYY-MM-DD), shelfLifeValue(保质期数字), shelfLifeUnit(天/月/年), location(存放建议), quantity(数量数字), unit(单位)。看不清或不存在的字段返回空字符串；不要推测日期；无法识别的商品名返回空字符串。' },
            { type: 'image_url', image_url: { url: imageDataUrl, detail: 'high' } },
          ],
        }],
      }),
    })
  } catch (error) {
    if (error instanceof TypeError) throw new Error('无法连接识图 API。请检查地址和网络，并确认接口允许网页跨域访问（CORS）')
    throw error
  }

  const payload = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(payload.error?.message || payload.message || `识图 API 请求失败（${response.status}）`)
  const content = payload.choices?.[0]?.message?.content
  const text = Array.isArray(content) ? content.map(part => part.text || '').join('\n') : content
  if (!text) throw new Error('API 没有返回识别结果，请检查模型是否支持图片识别')
  const json = String(text).replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '').trim()
  try {
    return JSON.parse(json)
  } catch {
    throw new Error('无法解析 API 返回内容，请确认接口返回 JSON 格式')
  }
}
