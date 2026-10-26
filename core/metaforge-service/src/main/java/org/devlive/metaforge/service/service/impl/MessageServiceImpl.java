package org.devlive.metaforge.service.service.impl;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.common.utils.AiSupportUtils;
import org.devlive.metaforge.common.utils.JsonUtils;
import org.devlive.metaforge.service.body.UserQuestionBody;
import org.devlive.metaforge.service.entity.ChatEntity;
import org.devlive.metaforge.service.entity.MessageEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.enums.MessageType;
import org.devlive.metaforge.service.model.AiModel;
import org.devlive.metaforge.service.repository.BaseRepository;
import org.devlive.metaforge.service.repository.MessageRepository;
import org.devlive.metaforge.service.repository.UserRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.MessageService;
import org.devlive.sdk.openai.OpenAiClient;
import org.devlive.sdk.openai.entity.UsageEntity;
import org.devlive.sdk.openai.model.CompletionModel;
import org.devlive.sdk.openai.model.MessageModel;
import org.devlive.sdk.openai.response.ChatResponse;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
        justification = "I prefer to suppress these FindBugs warnings")
public class MessageServiceImpl
        implements MessageService
{
    private final MessageRepository repository;
    private final UserRepository userRepository;
    private final Environment environment;

    public MessageServiceImpl(MessageRepository repository, UserRepository userRepository, Environment environment)
    {
        this.repository = repository;
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @Override
    public CommonResponse<MessageEntity> saveOrUpdate(BaseRepository repository, MessageEntity configure)
    {
        String openApiHost = environment.getProperty("metaforge.openai.backend");
        String openApiToken = environment.getProperty("metaforge.openai.token");
        String openApiModel = environment.getProperty("metaforge.openai.model");
        long openApiTimeout = Long.parseLong(environment.getProperty("metaforge.openai.timeout"));

        UserEntity user = UserDetailsService.getUser();
        MessageEntity questionMessage = MessageEntity.builder()
                .user(user)
                .chat(configure.getChat())
                .model(StringUtils.isNotEmpty(configure.getModel()) ? configure.getModel() : openApiModel)
                .content(configure.getContent())
                .name(UUID.randomUUID().toString())
                .type(MessageType.question)
                .build();
        this.repository.save(questionMessage);
        int contentCount = 5;
        boolean getContent = true;
        if (StringUtils.isNotEmpty(configure.getModel())) {
            openApiModel = configure.getModel();
        }
        // If user-defined configuration, use user configuration information
        if (StringUtils.isNotEmpty(user.getChatConfigure())) {
            AiModel aiModel = JsonUtils.toObject(user.getChatConfigure(), AiModel.class);
            if (StringUtils.isNotEmpty(aiModel.getToken())) {
                openApiHost = aiModel.getHost();
                openApiToken = aiModel.getToken();
            }
            if (aiModel.getTimeout() > 0) {
                openApiTimeout = aiModel.getTimeout();
            }
            if (aiModel.getContentCount() > 0) {
                contentCount = aiModel.getContentCount();
            }
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(openApiTimeout, TimeUnit.SECONDS)
                .writeTimeout(openApiTimeout, TimeUnit.SECONDS)
                .readTimeout(openApiTimeout, TimeUnit.SECONDS)
                .build();
        try (OpenAiClient openAiClient = OpenAiClient.builder()
                .apiHost(openApiHost)
                .apiKey(openApiToken)
                .client(okHttpClient)
                .build()) {
            List<org.devlive.sdk.openai.entity.MessageEntity> messages = new ArrayList<>();
            // Get the context in the conversation * 2
            if (getContent) {
                this.repository.findTopByChatOrderByCreateTimeDesc(configure.getChat(), questionMessage.getId(), Pageable.ofSize(contentCount * 2))
                        .stream()
                        .sorted(Comparator.comparing(MessageEntity::getId))
                        .forEach(message -> {
                            String role = message.getType() == MessageType.question ? MessageModel.USER.getName() : MessageModel.ASSISTANT.getName();
                            org.devlive.sdk.openai.entity.MessageEntity completionMessage = org.devlive.sdk.openai.entity.MessageEntity.builder()
                                    .role(role)
                                    .content(message.getContent())
                                    .build();
                            messages.add(completionMessage);
                        });
            }
            org.devlive.sdk.openai.entity.MessageEntity message = org.devlive.sdk.openai.entity.MessageEntity.builder()
                    .role(MessageModel.USER.getName())
                    .content(questionMessage.getContent())
                    .build();
            messages.add(message);
            org.devlive.sdk.openai.entity.ChatEntity chatCompletion = org.devlive.sdk.openai.entity.ChatEntity.builder()
                    .messages(messages)
                    .maxTokens(2048)
                    .model(CompletionModel.valueOf(openApiModel))
                    .build();
            ChatResponse chatCompletionResponse = openAiClient.createChatCompletion(chatCompletion);
            List<String> answer = Lists.newArrayList();
            chatCompletionResponse.getChoices()
                    .forEach(e -> answer.add(e.getMessage().getContent()));
            UsageEntity usage = chatCompletionResponse.getUsage();
            UserEntity aiUser = this.userRepository.findByUsernameAndSystemTrue("Ai");
            MessageEntity answerEntity = MessageEntity.builder()
                    .user(aiUser)
                    .chat(configure.getChat())
                    .model(openApiModel)
                    .content(String.join("\n", answer))
                    .type(MessageType.answer)
                    .promptTokens(usage.getPromptTokens())
                    .completionTokens(usage.getCompletionTokens())
                    .totalTokens(usage.getTotalTokens())
                    .name(UUID.randomUUID().toString())
                    .build();
            this.repository.save(answerEntity);
            return CommonResponse.success(answerEntity);
        }
        catch (Exception e) {
            return CommonResponse.failure(e.getMessage());
        }
    }

    @Override
    public CommonResponse<List<MessageEntity>> getMessageByChatAndUser(Long chatId)
    {
        ChatEntity chat = ChatEntity.builder()
                .id(chatId)
                .build();
        return CommonResponse.success(this.repository.findAllByChatOrderByCreateTimeAsc(chat));
    }

    @Override
    public CommonResponse<MessageEntity> aiReply(UserQuestionBody configure)
    {
        String openApiHost = environment.getProperty("metaforge.openai.backend");
        String openApiToken = environment.getProperty("metaforge.openai.token");
        String openApiModel = environment.getProperty("metaforge.openai.model");
        long openApiTimeout = Long.parseLong(environment.getProperty("metaforge.openai.timeout"));
        UserEntity user = this.userRepository.findById(UserDetailsService.getUser().getId()).get();
        if (StringUtils.isNotEmpty(configure.getModel())) {
            openApiModel = configure.getModel();
        }
        // If user-defined configuration, use user configuration information
        String forwardContent = null;
        if (StringUtils.isNotEmpty(user.getChatConfigure())) {
            AiModel aiModel = JsonUtils.toObject(user.getChatConfigure(), AiModel.class);
            if (StringUtils.isNotEmpty(aiModel.getToken())) {
                openApiHost = aiModel.getHost();
                openApiToken = aiModel.getToken();
            }
            if (aiModel.getTimeout() > 0) {
                openApiTimeout = aiModel.getTimeout();
            }
        }
        try {
            AiSupportUtils.AiSupportEnum type = AiSupportUtils.AiSupportEnum.valueOf(configure.getTransType());
            String replaceContent = AiSupportUtils.getValue(configure.getLocale(), type);
            Properties properties = new Properties();
            properties.put("sql", configure.getContent());
            if (ObjectUtils.isNotEmpty(configure.getEngine())) {
                properties.put("engine", configure.getEngine());
            }
            if (ObjectUtils.isNotEmpty(configure.getError())) {
                properties.put("error", configure.getError());
            }
            StrSubstitutor sub = new StrSubstitutor(properties);
            forwardContent = sub.replace(replaceContent);
        }
        catch (Exception exception) {
            log.warn("Ai type not set, ignore .");
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(openApiTimeout, TimeUnit.SECONDS)
                .writeTimeout(openApiTimeout, TimeUnit.SECONDS)
                .readTimeout(openApiTimeout, TimeUnit.SECONDS)
                .build();
        try (OpenAiClient openAiClient = OpenAiClient.builder()
                .apiHost(openApiHost)
                .apiKey(openApiToken)
                .client(okHttpClient)
                .build()) {
            List<org.devlive.sdk.openai.entity.MessageEntity> messages = Lists.newArrayList(org.devlive.sdk.openai.entity.MessageEntity.builder()
                    .role(MessageModel.USER.getName())
                    .content(forwardContent)
                    .build());
            org.devlive.sdk.openai.entity.ChatEntity chatCompletion = org.devlive.sdk.openai.entity.ChatEntity.builder()
                    .messages(messages)
                    .maxTokens(2048)
                    .model(CompletionModel.valueOf(openApiModel))
                    .build();
            ChatResponse chatCompletionResponse = openAiClient.createChatCompletion(chatCompletion);
            List<String> answer = Lists.newArrayList();
            chatCompletionResponse.getChoices()
                    .forEach(e -> answer.add(e.getMessage().getContent()));
            UsageEntity usage = chatCompletionResponse.getUsage();
            MessageEntity answerEntity = MessageEntity.builder()
                    .model(openApiModel)
                    .content(String.join("\n", answer))
                    .type(MessageType.answer)
                    .promptTokens(usage.getPromptTokens())
                    .completionTokens(usage.getCompletionTokens())
                    .totalTokens(usage.getTotalTokens())
                    .name(UUID.randomUUID().toString())
                    .build();
            return CommonResponse.success(answerEntity);
        }
        catch (Exception e) {
            return CommonResponse.failure(e.getMessage());
        }
    }
}
