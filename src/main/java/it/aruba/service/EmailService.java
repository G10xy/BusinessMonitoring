package it.aruba.service;


import it.aruba.model.kafka.UpsellingServiceDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;
    private static final String ENCODING = "UTF-8";

    @Value("${mail.from}")
    private String fromEmail;
    @Value("${mail.to}")
    private String mailTo;
    @Value("${mail.enable}")
    private Boolean enable;

    public void sendEmail(UpsellingServiceDTO dto) throws MessagingException, MailException {
        if (!enable) {
            log.warn("Email service disabled, to enable it set mail.enable=true in properties");
            return;
        }
        log.info("Preparing to send email to {}", mailTo);
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, ENCODING);
            helper.setFrom(fromEmail);
            helper.setTo(mailTo);
            helper.setSubject("Upselling opportunity for customer " + dto.customerId());
            helper.setText("Since customer whose id is " + dto.customerId() + " has been subscribed to " + dto.upsellingService() +
                    " service for more than 3 years, it is time to give our clients the opportunity for un upselling subscription.", true);

            emailSender.send(message);
            log.info("Email successfully sent to {}", mailTo);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send email to {}", mailTo, e);
            throw e;
        }
    }
}
