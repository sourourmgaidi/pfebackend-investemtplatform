package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Prospect;

@Service
public class MessageService {

    private final OllamaService ollamaService;

    @Value("${app.platform.url:https://investplatform.com}")
    private String platformUrl;

    @Value("${app.sender.name:The InvestPlatform Team}")
    private String senderName;

    @Value("${app.sender.title:Investment Strategist}")
    private String senderTitle;

    public MessageService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    // ─── 1. Generate plain-text body via Ollama ───────────────────────────────
    public String generate(Prospect p) {
        String prompt =
                "You are a professional email copywriter for a fintech investment platform called InvestPlatform.\n" +
                        "Write a SHORT, MODERN, PERSONALIZED prospecting email body in English.\n\n" +
                        "Prospect details:\n" +
                        "- First name: " + p.getName() + "\n" +
                        "- Company: " + p.getCompany() + "\n" +
                        "- Sector: " + p.getCategory() + "\n" +
                        "- City: " + p.getCity() + "\n" +
                        "- Interest Level: " + p.getInterestLevel() + "\n\n" +
                        "STRICT RULES — follow every one of them:\n" +
                        "1. Start directly with: 'Dear [prospect first name],'\n" +
                        "2. Write exactly 3 short paragraphs — no bullet points, no numbered lists\n" +
                        "3. Paragraph 1: one sentence introducing InvestPlatform\n" +
                        "4. Paragraph 2: two sentences on 2 benefits relevant to their sector\n" +
                        "5. Paragraph 3: one clear call-to-action (suggest a call next week)\n" +
                        "6. End with EXACTLY this sign-off on its own line: 'Best regards,' then on the next line: '" + senderName + "'\n" +
                        "7. Do NOT include a subject line\n" +
                        "8. Do NOT use placeholders like [Your Name] or [Name] anywhere\n" +
                        "9. Do NOT invent statistics or claims\n" +
                        "10. Maximum 120 words\n" +
                        "11. Output ONLY the email body text — nothing else, no explanations, no markdown";

        String generated = ollamaService.generateMessage(prompt);

        // Clean up common Ollama artifacts
        if (generated != null) {
            generated = generated.trim()
                    .replaceAll("(?i)subject:.*\\n", "")           // remove subject line if model added one
                    .replaceAll("\\[Your (Full )?Name\\]", senderName)  // fix leftover placeholders
                    .replaceAll("\\[Name\\]", senderName)
                    .replaceAll("#\\d+", "")                        // remove stray tokens like #1
                    .trim();
        }

        if (generated == null || generated.length() < 30) {
            generated = fallbackText(p);
        }

        return generated;
    }

    // ─── 2. Wrap plain text in a polished HTML email ──────────────────────────
    public String generateHtml(Prospect p) {
        String bodyText = generate(p);

        // Convert newlines to <br> for HTML rendering
        String htmlBody = bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n\n", "</p><p style='margin:0 0 16px 0;'>")
                .replace("\n", "<br>");

        return "<!DOCTYPE html>" +
                "<html lang='en'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>InvestPlatform</title></head>" +
                "<body style='margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,sans-serif;'>" +

                // ── Outer wrapper
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f6f9;padding:40px 0;'>" +
                "<tr><td align='center'>" +

                // ── Card
                "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;" +
                "border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +

                // ── Header
                "<tr><td style='background:#0d1b2a;padding:32px 40px;text-align:center;'>" +
                "<span style='font-size:26px;font-weight:800;color:#4da6ff;letter-spacing:1px;'>" +
                "InvestPlatform</span>" +
                "<p style='margin:6px 0 0;color:#8ab4d4;font-size:13px;letter-spacing:2px;text-transform:uppercase;'>" +
                "Smart Investment Management</p>" +
                "</td></tr>" +

                // ── Accent bar
                "<tr><td style='height:4px;background:linear-gradient(to right,#4da6ff,#0057b8);'></td></tr>" +

                // ── Body
                "<tr><td style='padding:40px 40px 32px;color:#2d3748;font-size:15px;line-height:1.7;'>" +
                "<p style='margin:0 0 16px 0;'>" + htmlBody + "</p>" +
                "</td></tr>" +

                // ── CTA Button
                "<tr><td style='padding:0 40px 40px;text-align:center;'>" +
                "<a href='" + platformUrl + "' " +
                "style='display:inline-block;background:#0057b8;color:#ffffff;text-decoration:none;" +
                "padding:14px 36px;border-radius:8px;font-size:15px;font-weight:700;letter-spacing:0.5px;'>" +
                "Discover InvestPlatform →</a>" +
                "</td></tr>" +

                // ── Divider
                "<tr><td style='padding:0 40px;'>" +
                "<hr style='border:none;border-top:1px solid #e2e8f0;'>" +
                "</td></tr>" +

                // ── Footer
                "<tr><td style='padding:24px 40px;text-align:center;'>" +
                "<p style='margin:0;font-size:12px;color:#a0aec0;'>" +
                "© 2025 InvestPlatform — " +
                "<a href='mailto:contact@investplatform.com' style='color:#4da6ff;text-decoration:none;'>contact@investplatform.com</a>" +
                "</p>" +
                "<p style='margin:6px 0 0;font-size:11px;color:#cbd5e0;'>" +
                "You are receiving this email because you are a registered prospect. " +
                "<a href='#' style='color:#a0aec0;'>Unsubscribe</a>" +
                "</p>" +
                "</td></tr>" +

                "</table>" + // end card
                "</td></tr></table>" + // end outer
                "</body></html>";
    }

    // ─── 3. Fallback (if Ollama fails or returns garbage) ────────────────────
    private String fallbackText(Prospect p) {
        return "Dear " + p.getName() + ",\n\n" +
                "I'm reaching out to introduce InvestPlatform — a solution built to centralize " +
                "investment opportunities and simplify deal management for professionals in the " +
                p.getCategory() + " sector.\n\n" +
                "Our platform gives you real-time pipeline visibility and automated performance " +
                "tracking, so your team at " + p.getCompany() + " can focus on decisions that matter.\n\n" +
                "I'd love to schedule a quick call next week to walk you through it. " +
                "Would Tuesday or Thursday afternoon work for you?\n\n" +
                "Best regards,\n" + senderName + "\n" + senderTitle + " — InvestPlatform";
    }
}