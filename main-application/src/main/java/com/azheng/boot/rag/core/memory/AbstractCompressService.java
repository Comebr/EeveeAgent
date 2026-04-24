package com.azheng.boot.rag.core.memory;


import com.azheng.boot.rag.core.prompt.PromptTemplateLoader;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.azheng.boot.rag.constant.RAGConstant.CONVERSATION_SUMMARY_TEMPLATE_PATH;


/*多轮对话-摘要压缩*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AbstractCompressService {

    @Resource(name = "qwen-turbo")
    private ChatModel chatModel;

    private final PromptTemplateLoader promptTemplateLoader;

    private final HistoryAbstractStore historyAbstractStore;

    public static final Integer MAX_ROUNDS = 6;

    /**
     * 阈值判断：轮次
     * 统计
     */
    public RoundResult  judgeThresholdByRounds(List<ChatMessage> messages){
        int round = 0;
        int i = 0;
        List<ChatMessage> validRounds = new ArrayList<>();
        while (i < messages.size()-1){
            ChatMessage curr = messages.get(i);
            ChatMessage next = messages.get(i+1);
            if (curr.type().equals(ChatMessageType.USER) && next.type().equals(ChatMessageType.AI)) {
                validRounds.add(curr);
                validRounds.add(next);
                round++;
                i+=2;
            }else {
                messages.remove(i); // 移除
                i++;  // 跳过不完整的轮次
            }
        }
        return round >= MAX_ROUNDS ? new RoundResult(true,round,messages ) : new RoundResult(false,round,messages );
    }


    /**
     * 执行压缩
     */
    public void CompressIfNeed(String conversationId, List<ChatMessage> messages){
        // 1.判断会话是否需要压缩+清理异常消息
        RoundResult roundResult = judgeThresholdByRounds(messages);
        if (!roundResult.isBeyond()){
            return;
        }
        int rounds = roundResult.rounds();
        int index = 2*(rounds-2);
        // 压缩前 rounds-2 轮
        /* 细节subList左闭右开！切N条就写N */
        //从纯轮次
        List<ChatMessage> validRounds = roundResult.validRounds();
        //滑出去的部分
        List<ChatMessage> extractedPart = validRounds.subList(0, index);
        String messagesToJson = ChatMessageSerializer.messagesToJson(extractedPart);
        //留在窗口的部分
        List<ChatMessage> remainingPart = validRounds.subList(index, validRounds.size());

        //1.生成多轮会话摘要
        try {
            String prompt = promptTemplateLoader.render(CONVERSATION_SUMMARY_TEMPLATE_PATH,
                    Map.of("messages", messagesToJson));
            SystemMessage systemMessage = SystemMessage.from(prompt);
            ChatResponse chatResponse = chatModel.chat(systemMessage);
            String summary = chatResponse.aiMessage().text();

            List<ChatMessage> list = List.of(SystemMessage.from(summary));

            //2.存入摘要记录
            historyAbstractStore.insert(conversationId,list);

            // 3.删除旧数据
            historyAbstractStore.updateSession(conversationId,remainingPart);
        } catch (Exception e) {
            log.error("多轮会话自动压缩执行发生异常：", e);
        }
    }



}
