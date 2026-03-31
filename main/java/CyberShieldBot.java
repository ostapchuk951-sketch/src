import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class CyberShieldBot extends TelegramLongPollingBot {

    // Отримуємо дані з системних змінних (Environment Variables)
    // На Render ти впишеш ці ключі в панелі керування
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String BOT_USERNAME = System.getenv("BOT_USERNAME"); 
    private static final String VT_API_KEY = System.getenv("VT_API_KEY");

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            String chatId = update.getMessage().getChatId().toString();
            String fileId = update.getMessage().getDocument().getFileId();
            String fileName = update.getMessage().getDocument().getFileName();

            sendTextMessage(chatId, "⏳ Отримав файл " + fileName + ". Починаю аналіз через VirusTotal API...");

            try {
                String fileUrl = getFileUrl(fileId);
                String scanId = scanFileWithVirusTotal(fileUrl);
                String report = getVirusTotalReport(scanId);
                
                sendTextMessage(chatId, "✅ Аналіз завершено для: " + fileName + "\n\nРезультат: " + report);

            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Помилка при перевірці: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
             String chatId = update.getMessage().getChatId().toString();
             sendTextMessage(chatId, "Привіт! Я Cyber-Shield Bot. 🛡\nКинь мені підозрілий файл, і я перевірю його через VirusTotal.");
        }
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getFileUrl(String fileId) throws TelegramApiException {
        org.telegram.telegrambots.meta.api.methods.GetFile getFileMethod = new org.telegram.telegrambots.meta.api.methods.GetFile();
        getFileMethod.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File file = execute(getFileMethod);
        return file.getFileUrl(getBotToken());
    }

    private String scanFileWithVirusTotal(String fileUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("url", fileUrl)
                .build();

        Request request = new Request.Builder()
                .url("https://www.virustotal.com/api/v3/urls")
                .post(formBody)
                .addHeader("x-apikey", VT_API_KEY)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("VirusTotal API error: " + response.code());
            JSONObject json = new JSONObject(response.body().string());
            return json.getJSONObject("data").getString("id");
        }
    }

    private String getVirusTotalReport(String scanId) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://www.virustotal.com/api/v3/analyses/" + scanId)
                .get()
                .addHeader("x-apikey", VT_API_KEY)
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Report error: " + response.code());
            JSONObject json = new JSONObject(response.body().string());
            
            JSONObject stats = json.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats");
            int malicious = stats.getInt("malicious");
            int harmless = stats.getInt("harmless");
            
            if (malicious > 0) {
                return "🚨 УВАГА! Знайдено " + malicious + " загроз(и). Файл НЕБЕЗПЕЧНИЙ.";
            } else if (harmless > 0) {
                return "🟢 Файл виглядає чистим (перевірено " + harmless + " антивірусами).";
            } else {
                return "⚪ Результати ще обробляються. Спробуй ще раз за хвилину.";
            }
        }
    }
}
