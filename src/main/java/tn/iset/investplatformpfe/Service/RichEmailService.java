package tn.iset.investplatformpfe.Service;

// src/main/java/tn/iset/investplatformpfe/Service/RichEmailService.java

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
                String htmlContent = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
                        "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'>" +
                        "<tr><td align='center'>" +
                        "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +

                        // Header
                        "<tr><td style='background:#0d1b2a;padding:32px 40px;text-align:center;'>" +
                        "<span style='font-size:26px;font-weight:800;color:#4da6ff;letter-spacing:1px;'>InvestPlatform</span>" +
                        "<p style='margin:6px 0 0;color:#8ab4d4;font-size:13px;letter-spacing:2px;text-transform:uppercase;'>Smart Investment Management</p>" +
                        "</td></tr>" +

                        // Accent bar
                        "<tr><td style='height:4px;background:linear-gradient(to right,#4da6ff,#0057b8);'></td></tr>" +

                        // Body text
                        "<tr><td style='padding:40px 40px 24px;color:#2d3748;font-size:15px;line-height:1.7;'>" +
                        "<p style='margin:0;white-space:pre-line;'>" + content + "</p>" +
                        "</td></tr>" +

                        // Image
                        "<tr><td style='padding:0 40px 32px;'>" +
                        "<img src='cid:attachedImage' style='width:100%;max-width:520px;border-radius:10px;display:block;'/>" +
                        "</td></tr>" +

                        // CTA Button
                        "<tr><td style='padding:0 40px 40px;text-align:center;'>" +
                        "<a href='https://investplatform.com' style='display:inline-block;background:#0057b8;color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:15px;font-weight:700;'>Discover InvestPlatform →</a>" +
                        "</td></tr>" +

                        // Divider
                        "<tr><td style='padding:0 40px;'><hr style='border:none;border-top:1px solid #e2e8f0;'></td></tr>" +

                        // Footer
                        "<tr><td style='padding:24px 40px;text-align:center;'>" +
                        "<p style='margin:0;font-size:12px;color:#a0aec0;'>© 2025 InvestPlatform — <a href='mailto:contact@investplatform.com' style='color:#4da6ff;text-decoration:none;'>contact@investplatform.com</a></p>" +
                        "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e0;'>You are receiving this email because you are a registered prospect. <a href='#' style='color:#a0aec0;'>Unsubscribe</a></p>" +
                        "</td></tr>" +

                        "</table></td></tr></table></body></html>";

                helper.setText(htmlContent, true);
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                String mimeType = detectMimeType(imageName);
                helper.addInline("attachedImage", new ByteArrayResource(imageBytes), mimeType);
            } else {
                // Email HTML sans image — NOUVEAU DESIGN
                String htmlContent = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
                        "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,sans-serif;'>" +
                        "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'>" +
                        "<tr><td align='center'>" +
                        "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +

                        // Header
                        "<tr><td style='background:#0d1b2a;padding:32px 40px;text-align:center;'>" +
                        "<span style='font-size:26px;font-weight:800;color:#4da6ff;letter-spacing:1px;'>InvestPlatform</span>" +
                        "<p style='margin:6px 0 0;color:#8ab4d4;font-size:13px;letter-spacing:2px;text-transform:uppercase;'>Smart Investment Management</p>" +
                        "</td></tr>" +

                        // Accent bar
                        "<tr><td style='height:4px;background:linear-gradient(to right,#4da6ff,#0057b8);'></td></tr>" +

                        // Body
                        "<tr><td style='padding:40px 40px 32px;color:#2d3748;font-size:15px;line-height:1.7;'>" +
                        "<p style='margin:0;white-space:pre-line;'>" + content + "</p>" +
                        "</td></tr>" +

                        // CTA Button
                        "<tr><td style='padding:0 40px 40px;text-align:center;'>" +
                        "<a href='https://investplatform.com' style='display:inline-block;background:#0057b8;color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:15px;font-weight:700;'>Discover InvestPlatform →</a>" +
                        "</td></tr>" +

                        // Divider
                        "<tr><td style='padding:0 40px;'><hr style='border:none;border-top:1px solid #e2e8f0;'></td></tr>" +

                        // Footer
                        "<tr><td style='padding:24px 40px;text-align:center;'>" +
                        "<p style='margin:0;font-size:12px;color:#a0aec0;'>© 2025 InvestPlatform — <a href='mailto:contact@investplatform.com' style='color:#4da6ff;text-decoration:none;'>contact@investplatform.com</a></p>" +
                        "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e0;'>You are receiving this email because you are a registered prospect. <a href='#' style='color:#a0aec0;'>Unsubscribe</a></p>" +
                        "</td></tr>" +

                        "</table></td></tr></table></body></html>";
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