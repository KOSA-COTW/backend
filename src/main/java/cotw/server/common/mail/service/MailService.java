package cotw.server.common.mail.service;

import java.util.Map;

public interface MailService {
    void sendText(String to, String subject, String text);
    void sendHtml(String to, String subject, String html);
    void sendHtmlWithAttachment(String to, String subject, String html,
                                String filename, byte[] bytes, String contentType);
    void sendTemplate(String to, String subject, String template, Map<String,Object> model);
}

