package com.gymtracker.core.bot;

import com.gymtracker.core.Repository.AppUserRepository;
import com.gymtracker.core.Repository.ExerciseRepository;
import com.gymtracker.core.Service.WorkoutService; // Импортируем твой сервис!
import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.Exercise;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Component
public class GymBot extends TelegramLongPollingBot {

    private final AppUserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutService workoutService; // Внедряем сервис для инвайтов
    private final String botUsername;

    public GymBot(ExerciseRepository exerciseRepository,
                  AppUserRepository userRepository,
                  WorkoutService workoutService, // Spring сам подставит его сюда
                  @Value("${telegram.bot.token}") String botToken,
                  @Value("${telegram.bot.username}") String botUsername){
        super(botToken);
        this.userRepository = userRepository;
        this.botUsername = botUsername;
        this.exerciseRepository = exerciseRepository;
        this.workoutService = workoutService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            String userName = update.getMessage().getFrom().getUserName();


            if (messageText.startsWith("/start")) {
                Optional<AppUser> userOptional = userRepository.findByTelegramId(chatId);
                boolean isNewUser = userOptional.isEmpty();


                if (isNewUser) {
                    AppUser newUser = new AppUser();
                    newUser.setTelegramId(chatId);
                    newUser.setTelegramUserName(userName);
                    userRepository.save(newUser);
                }

                if (messageText.contains("ref_")) {
                    try {

                        String inviterIdStr = messageText.substring(messageText.indexOf("ref_") + 4);
                        Long inviterId = Long.parseLong(inviterIdStr);


                        workoutService.sendFriendRequest(inviterId, chatId);

                        sendTextMessage(chatId, "🔥 Добро пожаловать в GymTracker по приглашению! Твой профиль создан. Твой друг уже получил запрос на дружбу, открой Профиль в Web App и нажми кнопку 'Принять'!");
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (messageText.contains("program_")) {
                    try {
                        String programIdStr = messageText.substring(messageText.indexOf("program_") + 8).trim();
                        Long programId = Long.parseLong(programIdStr);

                        workoutService.copySharedProgram(programId, chatId);

                        sendTextMessage(chatId, "Программа добавлена в твой GymTracker. Открой Web App и начинай тренироваться.");
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendTextMessage(chatId, "Не получилось добавить программу по ссылке. Попроси отправить ссылку еще раз.");
                        return;
                    }
                }

                if (isNewUser) {
                    sendTextMessage(chatId, "Привет! Твой профиль в GymTracker успешно создан. Нажми кнопку 'Зал' внизу, чтобы начать тренироваться!");
                } else {
                    sendTextMessage(chatId, "С возвращением в GymTracker! Твой зал заждался. Погнали тренироваться!");
                }
            }


            else if (messageText.equals("/exercises")) {
                List<Exercise> exercises = exerciseRepository.findAll();
                StringBuilder sb = new StringBuilder();
                sb.append("🏋️‍♂️ *Доступные упражнения:*\n\n");
                for (Exercise ex : exercises) {
                    sb.append("• *").append(ex.getName()).append("*\n");
                    sb.append(ex.getDescription()).append("\n\n");
                }
                sendMarkdownMessage(chatId, sb.toString());
            }
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMarkdownMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }
}
