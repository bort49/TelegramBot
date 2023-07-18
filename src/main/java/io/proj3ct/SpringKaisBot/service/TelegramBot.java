package io.proj3ct.SpringKaisBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.SpringKaisBot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.w3c.dom.ls.LSOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.List;

import static io.proj3ct.SpringKaisBot.SpringKaisBotApplication.blacklist;


@Component //позволит автоматом создать экземпляр спрингу
public class TelegramBot extends TelegramLongPollingBot {
    //LongPollinBot в отличии от WebHookBot не получает уведомления если ктото что-то написал в бот
    //он сам периодически проверяет, но он проще

    final BotConfig config;
    final int botAdmin; //final int botAdmin = 397611532;

    public TelegramBot(BotConfig config) {
        this.config = config;
        botAdmin = Integer.parseInt(this.config.getBotAdmin());
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() { return config.getToken();}

// TODO: 30.01.2023
//  1. если админов несколько, то надо сделать List<String> botAdmins
//  2. Надо сделать список заблокированных chatID в файле. Если входное сообщение пришло от ChatID из этого списка,
//  то никак не обрабатываеть его.
//  3. Для KAIS надо кидать ответы на внутренние сообщения на ftp с ChatID отправителя в имени файла.
//  KAIS бедет проверять файлы и искать сообщения от своих операторов



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
         //   String chart_with_downwards_trend = "\u1F4C9";
//            switch (messageText) {
//                case "/start":
//                    StartCommandReceived(chatId, update.getMessage().getChat().getFirstName());
//                    break;
//                case "/help":
//                    StartCommandReceived(chatId, update.getMessage().getChat().getFirstName());
//                    break;
//                default:
//                    SendMessage(chatId, "Эта команда пока не поддерживается. Используй /start или /help");
//            }

            //пишем в файл нолученное сообщение
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime now = LocalDateTime.now();
            String log_string = now.format(formatter)+","
                                +String.valueOf(chatId)+","
                                +update.getMessage().getChat().getFirstName()+" "
                                +update.getMessage().getChat().getLastName()+","
                                +"\""+messageText+"\" \n";

            Path path = Paths.get("log.txt");

            try {
                Files.writeString(path,
                                  log_string,
                                  StandardOpenOption.APPEND,
                                  StandardOpenOption.CREATE);

                   } catch (IOException e) {
            }



            boolean already_answer_key = false;

            try {
                if (update.getMessage().getReplyToMessage().hasText()) {
                    String replyed_message = update.getMessage().getReplyToMessage().getText();
                    if (replyed_message.contains("Внутреннее сообщение в программе") && replyed_message.contains("От: ")) {
                        //тогда пишем файл на ftp с именем chatId. В нем бует ответ оператору после "От: " в replyed_message
                 //       System.out.println(replyed_message);
                        SendMessage(chatId, update.getMessage().getChat().getFirstName() + ", я пока не умею отправлять ответ на полученное из программы сообщение " + EmojiParser.parseToUnicode(":confused:"));
                        already_answer_key = true;
                    }
                }
            } catch (Exception e) {}


            if  (!already_answer_key) {
                try {
                    for (String s : blacklist) {
                        if (messageText.contains(s)) {
                            SendMessage(chatId, update.getMessage().getChat().getFirstName() + ", мое мнение о тебе - " + EmojiParser.parseToUnicode(":sneezing_face:") + EmojiParser.parseToUnicode(":chart_with_downwards_trend:"));
                            already_answer_key = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                }
            }


            // System.out.println(messageText);

            if (!already_answer_key) { //не отправляли ответ о том, что это ответ на сообщение в программе или мат

                if (messageText.toLowerCase().contains("start") || messageText.toLowerCase().contains("help") ||
                    messageText.toLowerCase().contains("ты кто") || messageText.toLowerCase().contains("кто ты")) {
                    StartCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                else if ((messageText.toLowerCase().contains("stop_bot") || messageText.toLowerCase().contains("stopbot")) && (chatId == botAdmin)) {
                    SendMessage(chatId, "Принято! Бот будет остановлен.");
                    System.exit(0);
                }
                else if (messageText.contains("/send chatID:") && (chatId == botAdmin)) { //my chatId
                    sendRespondMessageFromAdmin(messageText);

                }
                else if (messageText.equalsIgnoreCase("привет") || messageText.equalsIgnoreCase("привет бот")) {
                    SendMessage(chatId, "Привет, "+update.getMessage().getChat().getFirstName()+". Хорошего дня тебе!");

                }
                else if (messageText.equalsIgnoreCase("и тебе") || messageText.equalsIgnoreCase("спасибо, и тебе") || messageText.equalsIgnoreCase("спасибо")) {
                    SendMessage(chatId, "И тебе спасибо, "+update.getMessage().getChat().getFirstName() +" "+ EmojiParser.parseToUnicode(":slight_smile:"));

                }

                else {
                    sendCommonRespond(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName(), messageText);
                }

            }
        }

    }


    private void sendRespondMessageFromAdmin(String messageText) {
        try {
            String respondMessage = messageText.substring(14); //тут respondedChatId+'\n'+textMessage
            int index = respondMessage.indexOf("\n");
            int respondedChatId = Integer.valueOf(respondMessage.substring(0,index));
            respondMessage = respondMessage.substring(index+1);

            SendMessage(respondedChatId, respondMessage);
            SendMessage(botAdmin, "Отправлено для chatID="+respondedChatId);
        } catch (Exception e) {

        }
    }

    private void sendCommonRespond(long chatId, String firstName, String lastName, String messageText) {
        if (messageText.length() <= 15) {
            SendMessage(chatId, firstName + ", эта команда пока не поддерживается в автоматическом режиме " + EmojiParser.parseToUnicode(":confused:") + "\nИспользуй команды /start или /help");
        } else {
            SendMessage(chatId, firstName + ", эта команда пока не поддерживается в автоматическом режиме " + EmojiParser.parseToUnicode(":confused:") + "\nВам ответит оператор.");
        }

        if (botAdmin != chatId) { //and send message to botAdmin
            SendMessage(botAdmin, firstName + " " + lastName +
                                   "\nchatID: " + chatId + "\n" +
                                   messageText);
        }

    }


    private void StartCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ". Это Телеграм-бот для пользователей программы КАИС \"АвтоСтандарТ\"  www.kais.ru \n"
                                          +"Здесь ты можешь получать копии внутренних сообщений из программы КАИС. Для этого сделай соответствующую настройку в программе в окне SMS сообщений и добавь свой \nChat ID:  "
                                          +chatId;

        SendMessage(chatId, answer);
    }

    private void SendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            //logs here
        }
    }


}
