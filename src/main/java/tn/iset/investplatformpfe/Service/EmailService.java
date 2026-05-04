package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private boolean testMode = false;

    public boolean send(String to, String content) {
        try {

            if (testMode) {
                to = "your_test_email@gmail.com";
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Opportunity Proposal");
            message.setText(content);

            mailSender.send(message);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
