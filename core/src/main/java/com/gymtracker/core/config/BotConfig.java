package com.gymtracker.core.config;

import com.gymtracker.core.bot.GymBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(GymBot gymBot) throws TelegramApiException {
        // Инициализируем API телеграма
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        botsApi.registerBot(gymBot);

        return botsApi;
    }
}