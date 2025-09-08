package cotw.server.common.mail.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine; // 템플릿 사용 시

    @Value("${app.mail.from}")
    private String from;


    @Async("mailExecutor")
    @Override
    public void sendText(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }

    @Async("mailExecutor")
    @Override
    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML
            // 인라인 이미지 예시:
            // helper.addInline("logo", new ClassPathResource("mail/logo.png"));
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to send mail", e);
        }
    }

    @Async("mailExecutor")
    @Override
    public void sendHtmlWithAttachment(String to, String subject, String html,
                                       String filename, byte[] bytes, String contentType) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addAttachment(filename, new ByteArrayResource(bytes), contentType);
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Failed to send mail", e);
        }
    }

    @Async("mailExecutor")
    @Override
    public void sendTemplate(String to, String subject, String template, Map<String, Object> model) {
        Context ctx = new Context(Locale.KOREA);
        ctx.setVariables(model);
        String html = templateEngine.process(template, ctx); // templates/mail/{template}.html
        sendHtml(to, subject, html);
    }
}

