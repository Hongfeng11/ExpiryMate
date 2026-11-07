package com.expirymate.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String endpoint;
    private final String apiKey;
    private final String model;

    public AiService(RestClient.Builder builder, ObjectMapper mapper,
                     @Value("${app.ai.endpoint:}") String endpoint,
                     @Value("${app.ai.api-key:}") String apiKey,
                     @Value("${app.ai.model:}") String model) {
        this.client = builder.build();
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        log.info("AI 配置：endpoint={}，model={}，apiKey={}", endpoint, model,
                apiKey.isBlank() ? "未配置" : "已配置(长度 " + apiKey.length() + ")");
    }

    public JsonNode chat(List<Map<String, String>> messages, List<Map<String, Object>> foods) {
        String today = java.time.LocalDate.now().toString();
        String foodsJson;
        try { foodsJson = mapper.writeValueAsString(foods == null ? List.of() : foods); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "清单数据格式无效"); }
        String system = "你是鲜期管家的家庭饮食助手。今天日期是 " + today
                + "。根据清单回答临期优先顺序和菜谱。过期食品标记为已过期且不建议食用，不得推荐为可食用食材。不要臆造保质期、食材或过敏信息。清单数据(JSON)：" + foodsJson;
        List<Map<String, String>> prompt = new ArrayList<>();
        prompt.add(Map.of("role", "system", "content", system));
        prompt.addAll(messages);
        return request(Map.of("model", model, "temperature", 0.4, "messages", prompt));
    }

    public JsonNode recognize(String imageDataUrl) {
        String prompt = "识别商品包装信息，只返回 JSON 对象，不要 Markdown。字段：name(商品名), category(食品/个护/保健品/清洁/其他), manufacturedOn(生产日期，YYYY-MM-DD), packageExpiresOn(包装到期日，YYYY-MM-DD), shelfLifeValue(保质期数字), shelfLifeUnit(天/月/年), location(存放建议), quantity(数量数字), unit(单位)。看不清或不存在的字段返回空字符串；不要推测日期。";
        Map<String, Object> userMessage = Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", prompt),
                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl, "detail", "high"))));
        JsonNode response = request(Map.of("model", model, "temperature", 0.1, "messages", List.of(userMessage)));
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        String text = content.isArray() ? content.findValuesAsText("text").stream().reduce((a,b) -> a + "\n" + b).orElse("") : content.asText("");
        text = text.replaceFirst("(?is)^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        try { return mapper.readTree(text); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "识图服务返回内容不是有效 JSON"); }
    }

    private JsonNode request(Map<String, Object> body) {
        if (endpoint.isBlank() || model.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 服务尚未配置，请管理员在后端配置 AI_API_URL 和 AI_MODEL");
        try {
            var request = client.post().uri(endpoint).header(HttpHeaders.CONTENT_TYPE, "application/json");
            if (!apiKey.isBlank()) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            return request.body(body).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            String respBody = ex.getResponseBodyAsString();
            if (respBody.length() > 500) respBody = respBody.substring(0, 500) + "...";
            log.warn("AI 上游返回 HTTP {}，endpoint={}，model={}，响应={}",
                    ex.getStatusCode().value(), endpoint, model, respBody);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务请求失败（HTTP " + ex.getStatusCode().value() + ")");
        } catch (Exception ex) {
            log.error("无法连接 AI 服务，endpoint={}，model={}，原因={}", endpoint, model, ex.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法连接 AI 服务，请检查后端配置和网络");
        }
    }
}
