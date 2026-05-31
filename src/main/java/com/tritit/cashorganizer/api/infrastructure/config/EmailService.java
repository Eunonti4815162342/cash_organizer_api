package com.tritit.cashorganizer.api.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@cashkeep.com}")
    private String fromAddress;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        if (mailSender == null) {
            log.warn("[DEV] SMTP not configured — password reset requested for {}. Configure spring.mail.host in production.", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("CashKeep \u2014 Restablece tu contrase\u00f1a");
            message.setText(buildEmailBody(resetToken));
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private String buildEmailBody(String token) {
        return """
                Hola,

                Has solicitado restablecer tu contrase\u00f1a en CashKeep.

                Tu c\u00f3digo de restablecimiento es:

                    %s

                Introduce este c\u00f3digo en la aplicaci\u00f3n. Expira en 1 hora.

                Si no has solicitado este cambio, ignora este mensaje.

                \u2014 Equipo CashKeep
                """.formatted(token);
    }
}
