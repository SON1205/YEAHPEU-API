package com.yeahpeu.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeahpeu.ai.controller.requset.PromptJudgeRequest;
import com.yeahpeu.ai.controller.response.PromptIncomingResponse;
import com.yeahpeu.ai.controller.response.PromptJudgeResponse;
import com.yeahpeu.ai.util.EntityToStringUtil;
import com.yeahpeu.budget.service.BudgetService;
import com.yeahpeu.budget.service.dto.BudgetSummaryDTO;
import com.yeahpeu.event.controller.condition.EventSearchCondition;
import com.yeahpeu.event.service.EventService;
import com.yeahpeu.event.service.dto.EventDto;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor
@Service
public class AiChatService {

    private final OpenAiChatModel openAiChatModel;



    private final EventService eventService;
    private final BudgetService budgetService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PromptJudgeResponse judgeResponse(PromptJudgeRequest promptRequest, Long userId) {
        String command =
                "결혼을 앞둔 부부가 사소한 문제로 곤란을 겪고 있을수도 있어. 이때의 문제의 원인을 cause, 아내의 입장을 wife, 남편의 입장을 husband key 와 함께 문장으로 보낼께. 부부들의 남은 {event-list} 를 확인하고 주어지는 문장에 대한 해결책을 찾아줘, 예를 들어 예비신부가 불만이 있다면 다음 남아있는 구체적인 일정의 준비는 신랑이 맡게 하는 등의 구체적인 가이드를 줘. \n"
                        + "만약 돈과 관련된 문제라면 {budget} 을 활용해 대답해줘."
                        + "이왕이면 예비신부 편을 들어줘. 다만 밑도끝도 없다기보다는 논리적으로 잘 풀어서 말해줘."
                        + "nativePrompt 에 숫자 혹은 의미없는 단어들이 들어올 수 있어. cause, wife, husband 중 이상한 것이 섞여 있으면 (해당 내용으로는 결과를 판단할 수 없습니다 😂 더 명료하게 써주면 도움을 줄 수 있습니다)라고 대답"
                        + "문장 : [{nativePrompt}] \n"
                        + "판단에 사용할 데이터 : [{event-list}]\n"
                        + "부부의 일정을 참고했다면, 날짜(년월일, T 이후는 제거)와 일정제목을 함께 언급해주기."
                        + "응답은 json 형식에 따라 응답해줘. 응답을 객체로 parsing 할꺼야 : {response-format}";

        PromptTemplate promptTemplate = new PromptTemplate(command);

        EventSearchCondition condition = new EventSearchCondition(false, 1, formatDateWithOffset(0),
                formatDateWithOffset(3));

        List<EventDto> eventListByWedding = eventService.getEventsByDateRange(condition, userId);
        BudgetSummaryDTO budgetSummary = budgetService.getBudgetSummary(userId);

        promptTemplate.add("event-list", EntityToStringUtil.convertList("남은 일정 정보", eventListByWedding));
        promptTemplate.add("budget", EntityToStringUtil.convert("예산 정보", budgetSummary));
        promptTemplate.add("nativePrompt", promptRequest);
        promptTemplate.add("response-format", judgeResponseFormat());

        String jsonStr = openAiChatModel.call(new Prompt(
                promptTemplate.create().toString(),
                OpenAiChatOptions.builder()
                        .withModel("gpt-4o")
                        .withTemperature(0.9f)
                        .build()
        )).getResult().getOutput().toString();
        //String jsonStr = openAiChatModel.call(promptTemplate.create()).getResult().getOutput().toString();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(jsonStr.substring(jsonStr.indexOf('{'), jsonStr.indexOf('}') + 1),
                    PromptJudgeResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private String judgeResponseFormat() {
        return "{" +
                "\"judge\" : 판단할 수 없다고 생각하는 경우(의미없는 숫자나 오타가 들어오는경우) 위에 기술한대로, (해당 내용으로는 결과를 판단할 수 없습니다 😂 더 명료하게 써주면 도움을 줄 수 있습니다) 라고 대답."
        + "잘 들어온 경우, 신랑과 신부의 과실비율을 'AI 판독결과, 과실비율 - 예신이 n: 예랑이 n' 으로 반환. (이 때 숫자의 범위는 0~100. 예신이는 신부, 예랑이는 신랑이야. 신랑은 무조건 51이상으로 해줘.) 이후 br태그 입력후 문제에 대한 해결을 ~습니다 체로 제시. (다 judge 안에 넣어)"
                + "}";
    }

    public PromptIncomingResponse generateIncomingEventResponse(Long userId) {
        String command = """
                결혼식을 준비 중이야.
                만약 "{week} == '없음'" 이면 incomingDay에 '현재 예정된 일정이 없어.'라고 대답.
                만약 "{week} == 0" 이면 '일정이 다가오고 있어!'라고 대답.
                만약 "{week} >= 1" 이면 '위 일정은 {week}주 남았어!'라고 대답.
                
                이 일정 {event}와 관련해서 창의적이고 실용적인 가이드를 하나 만들어줘.
                단순한 체크리스트 형식이 아니라, 실제로 도움이 될 만한 팁을 제공해줘.
                아래 예시처럼 작성하면 좋아:
                - 결혼식장 결정
                  - 결혼식장은 두 사람의 이야기가 담긴 장소면 더 의미 있어. 예를 들어 첫 데이트한 곳 근처라든지, 가족들이 오기 편한 곳이라든지!
                  - 예산뿐만 아니라 주차 공간, 동선, 조명, 하객 동선까지 고려해보자.
                - 상견례
                  - 분위기를 편안하게 만들려면, 가족끼리 공통된 관심사를 하나 찾아보는 것도 좋아!
                  - 식당 선택할 때 룸이 있는 곳을 추천! 서로 어색할 수도 있으니까 편한 분위기를 만들자.
                - 웨딩 어시스트
                  - 가방순이(신부 도우미) 선정
                    - 믿을 수 있는 친구 한 명을 선택해서 드레스 정리, 메이크업 체크 등을 부탁해보자!
                  - 축가 선정
                    - 축가를 고민 중이라면 두 사람의 연애스토리를 떠올리게 하는 노래를 선택하는 것도 방법!
                  - 사회자 선정
                    - 유머 감각이 좋은 친구 or 말을 조리 있게 잘하는 사람을 선택하는 게 중요해!
                    - 사회자와 사전 미팅을 해서 순서를 리허설해 보면 훨씬 매끄러운 진행이 가능해!
                
                위 예시는 참고만 해. AI가 창의적인 새로운 팁을 제시해줘.
                응답을 반드시 아래 JSON 형식으로 반환해:
                {response-format}
                """;

        PromptTemplate promptTemplate = getPromptTemplate(userId, command);

        String jsonStr = callAiModel(promptTemplate);

        return getPromptIncomingResponse(jsonStr);
    }

    private PromptIncomingResponse getPromptIncomingResponse(String jsonStr) {
        try {
            return objectMapper.readValue(jsonStr.substring(jsonStr.indexOf('{'), jsonStr.indexOf('}') + 1),
                    PromptIncomingResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PromptTemplate getPromptTemplate(Long userId, String command) {
        EventSearchCondition condition = new EventSearchCondition(false, 1, formatDateWithOffset(0),
                formatDateWithOffset(3));

        List<EventDto> eventListByWedding = eventService.getEventsByDateRange(condition, userId);

        return getTemplate(command, eventListByWedding);
    }

    private static PromptTemplate getTemplate(String command, List<EventDto> eventListByWedding) {
        PromptTemplate promptTemplate = new PromptTemplate(command);

        if (eventListByWedding.isEmpty()) {
            promptTemplate.add("week", "없음");
            promptTemplate.add("event", "향후 일정 없음");
        } else {
            long weeksBetween = ChronoUnit.WEEKS.between(ZonedDateTime.now(), eventListByWedding.getFirst().getDate());
            if (weeksBetween == 0) {
                promptTemplate.add("week", 0);
            } else {
                promptTemplate.add("week", weeksBetween);
            }
            promptTemplate.add("event", EntityToStringUtil.convertList("향후 일정", eventListByWedding));
        }

        promptTemplate.add("response-format",
                """
                        {
                             "incomingDay": "대략적으로 몇 주 남았는지 알려줘. '~ 남았어!'로 대답. 만약 숫자 0이면 '이번주에 해당 일정이 있어!'라고 대답",
                             "guide": "30자로 요약해 '~해보자' 체로 대답. 일정이 없다면 '남은 일정을 점검해볼까?'라고 대답."
                        }
                        """);
        return promptTemplate;
    }

    private String callAiModel(PromptTemplate promptTemplate) {
        try {
            return openAiChatModel.call(promptTemplate.create()).getResult().getOutput().toString();
        } catch (Exception e) {
            throw new RuntimeException("AI 응답을 가져오는 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String formatDateWithOffset(int yearsToAdd) {
        return LocalDate.now(ZoneId.of("Asia/Seoul"))
                .plusYears(yearsToAdd)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}