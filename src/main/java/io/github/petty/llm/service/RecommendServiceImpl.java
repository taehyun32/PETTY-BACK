package io.github.petty.llm.service;

import io.github.petty.llm.common.AreaCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {
    // RecommendController를 RecommendService로 분리
    private final VectorStoreService vectorStoreService;
    private final ChatService chatService;
    @Override
    public String recommend(Map<String, String> promptMap) {
        // 사용자 입력 기반 프롬프트 구성
        String userPrompt = buildPrompt(promptMap);

        // 지역 기반 필터 생성
        String location = promptMap.get("location");
        String filterExpression = buildRegion(location);

        List<Document> docs = vectorStoreService.findSimilarWithFilter(userPrompt, 5, filterExpression);

        // Gemini 연결

        try {
            String result = chatService.generateFromPrompt(userPrompt, docs);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 사용자 입력 기반 프롬프트 생성
    private String buildPrompt(Map<String, String> promptMap) {
        StringBuilder sb = new StringBuilder();

        String species = promptMap.get("species");
        String weight = promptMap.get("weight");
        String isDanger = promptMap.get("is_danger");
        String location = promptMap.get("location");

        sb.append(String.format("%s 정도의 몸무게를 가진 %s 종이에요.\n", weight, species));

        if(isDanger.equals("true")) {
            sb.append("맹견이에요\n");
        } else {
            sb.append("소형견, 중형견이에요\n");
        }

        sb.append(String.format("위치는 %s 근처이고, \n", location));

        String info = promptMap.get("info");
        if(info != null) {
            sb.append("추가로 이 장소의 설명은: ").append(info).append("\n");
        }
        return sb.toString();
    }

    // 지역 필터 조건 생성
    private String buildRegion(String location) {
        // ETC (지역 없을 때 대비)
        if (location == null || location.isBlank()) return "areaCode == 0";

        String[] parts = location.trim().split(" ");
        if (parts.length == 0) return "areaCode == 0";

        String areaName = parts[0];
        AreaCode areaCode = AreaCode.fromName(areaName);
        return "areaCode == %d".formatted(areaCode.getCode());
    }
}
