package tn.iset.investplatformpfe.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private boolean testMode = false;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    public boolean send(String to, String content) {
        return sendHtml(to, "Opportunity Proposal", content);
    }


    public boolean sendHtml(String to, String subject, String htmlContent) {
        try {
            if (testMode) {
                to = "your_test_email@gmail.com";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}