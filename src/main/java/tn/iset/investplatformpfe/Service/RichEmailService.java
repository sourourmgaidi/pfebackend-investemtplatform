package tn.iset.investplatformpfe.Service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class RichEmailService {

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendWithImage(String to, String subject, String content,
                                 String imageBase64, String imageName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            if (imageBase64 != null && !imageBase64.isBlank()) {
                // Construire un email HTML avec l'image intégrée
                String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
                        "<div style='background:#0f172a;padding:20px;border-radius:12px 12px 0 0;'>" +
                        "<h2 style='color:#38bdf8;margin:0;'>InvestPlatform</h2>" +
                        "</div>" +
                        "<div style='padding:24px;background:#fff;border:1px solid #e2e8f0;'>" +
                        "<p style='white-space:pre-line;color:#1e293b;line-height:1.7;'>" + content + "</p>" +
                        "<br/><img src='cid:attachedImage' style='max-width:100%;border-radius:8px;'/>" +
                        "</div>" +
                        "<div style='background:#f8fafc;padding:12px 24px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;border-top:none;'>" +
                        "<p style='color:#94a3b8;font-size:12px;margin:0;'>© 2025 InvestPlatform — Ne pas répondre à cet email</p>" +
                        "</div></div>";

                helper.setText(htmlContent, true);

                // Attacher l'image inline
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                String mimeType = detectMimeType(imageName);
                ByteArrayResource resource = new ByteArrayResource(imageBytes);
                helper.addInline("attachedImage", resource, mimeType);

            } else {
                // Email HTML sans image
                String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
                        "<div style='background:#0f172a;padding:20px;border-radius:12px 12px 0 0;'>" +
                        "<h2 style='color:#38bdf8;margin:0;'>InvestPlatform</h2>" +
                        "</div>" +
                        "<div style='padding:24px;background:#fff;border:1px solid #e2e8f0;'>" +
                        "<p style='white-space:pre-line;color:#1e293b;line-height:1.7;'>" + content + "</p>" +
                        "</div>" +
                        "<div style='background:#f8fafc;padding:12px 24px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;border-top:none;'>" +
                        "<p style='color:#94a3b8;font-size:12px;margin:0;'>© 2025 InvestPlatform — Ne pas répondre à cet email</p>" +
                        "</div></div>";
                helper.setText(htmlContent, true);
            }

            mailSender.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String detectMimeType(String fileName) {
        if (fileName == null) return "image/jpeg";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}

