package com.salahtech.BarberShop_Apis.Services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;


import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.reppsitories.DeviceTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final DeviceTokenRepository deviceTokenRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

     @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${notification.email.enabled}")
    private boolean emailEnabled;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.name:Barber Shop}")
    private String appName;

    @Autowired
    EmailService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Async
    public void sendWelcomeEmail(ApplicationUser user) {
        if (!emailEnabled) {
            logger.debug("Envoi d'emails désactivé");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue sur Barber Shop !");
            message.setText(
                String.format(
                    "Bonjour %s,\n\n" +
                    "Bienvenue sur notre plateforme Barber Shop !\n" +
                    "Votre compte a été créé avec succès.\n\n" +
                    "Vous pouvez maintenant explorer nos services et prendre rendez-vous avec nos barbiers professionnels.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Barber Shop",
                    user.getUsername(),
                    user.getFirstName() + " " + user.getLastName()
                )
            );

            mailSender.send(message);
            logger.info("Email de bienvenue envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de bienvenue à {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> model) {
        if (!emailEnabled) {
            log.debug("Envoi d'emails désactivé");
            return;
        }
        try {
            Context ctx = new Context();
            if (model != null && !model.isEmpty()) {
                ctx.setVariables(model);
            }
            // Autorise "booking-confirmation" ou "email/booking-confirmation"
            String tpl = (templateName != null && templateName.startsWith("email/"))
                    ? templateName
                    : "email/" + templateName;

            String html = templateEngine.process(tpl, ctx);
            sendEmail(to, subject, html, true);
            log.info("Template email '{}' envoyé à {}", tpl, to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du template email à {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.debug("Envoi d'emails désactivé");
            return;
        }
        try {
            // envoie en texte brut
            sendEmail(to, subject, text, false);
            log.info("Email texte envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email texte à {}: {}", to, e.getMessage(), e);
        }
    }

    
    public void sendVerificationEmail(ApplicationUser user) {
        String subject = "Vérification de votre compte Barber Shop";
        String verificationUrl = frontendUrl + "/auth/verify-email?token=" + user.getVerificationToken();
        
        String message = String.format("""
            Bonjour %s,
            
            Merci de vous être inscrit sur Barber Shop !
            
            Pour activer votre compte, veuillez cliquer sur le lien ci-dessous :
            %s
            
            Si vous n'avez pas créé de compte, vous pouvez ignorer cet email.
            
            Cordialement,
            L'équipe Barber Shop
            """, user.getFirstName(), verificationUrl);
        
        sendSimpleMessage(user.getEmail(), subject, message);
    }

    @Async
    public void sendPasswordResetEmail(com.salahtech.BarberShop_Apis.models.ApplicationUser user, String resetToken) {
        if (!emailEnabled) {
            logger.debug("Envoi d'emails désactivé");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Réinitialisation de votre mot de passe");
            message.setText(
                String.format(
                    "Bonjour %s,\n\n" +
                    "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                    "Votre token de réinitialisation: %s\n\n" +
                    "Ce token expire dans 30 minutes.\n" +
                    "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Barber Shop",
                    user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(),
                    resetToken
                )
            );

            mailSender.send(message);
            logger.info("Email de réinitialisation envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réinitialisation à {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedConfirmation(ApplicationUser user) {
        if (!emailEnabled) {
            logger.debug("Envoi d'emails désactivé");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Mot de passe modifié");
            message.setText(
                String.format(
                    "Bonjour %s,\n\n" +
                    "Votre mot de passe a été modifié avec succès.\n\n" +
                    "Si vous n'êtes pas à l'origine de cette modification, contactez-nous immédiatement.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe Barber Shop",
                    user.getUsername(),
                    user.getFirstName() + " " + user.getLastName()
                )
            );

            mailSender.send(message);
            logger.info("Email de confirmation de changement de mot de passe envoyé à: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation à {}: {}", user.getEmail(), e.getMessage());
        }
    }


    private void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
        } catch (Exception e) {
            // Log the error but don't fail the registration/reset process
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }

    /**
     * Envoyer un email de confirmation de réservation au client
     */
    public void sendBookingConfirmationToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);

            String htmlContent = templateEngine.process("email/booking-confirmation-client", context);

            sendEmail(
                to,
                "Confirmation de votre réservation - " + appName,
                htmlContent,
                true
            );

            log.info("Booking confirmation email sent to client: {}", to);

        } catch (Exception e) {
            log.error("Failed to deactivate device token for user {}: {}", e.getMessage(), e);
        }
    }

    /**
     * Supprimer le token d'un utilisateur
     */
    public void deleteDeviceToken(Long userId) {
        try {
            deviceTokenRepository.deleteByUserId(userId);
            log.info("Device token deleted for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to delete device token for user {}: {}", userId, e.getMessage(), e);
        }
    
    
 
            //log.error("Failed to send booking confirmation email to client: {}",userI e.getMessage(), e);
   }
    

    /**
     * Envoyer un email de notification de réservation au barbier
     */
    public void sendBookingNotificationToBarber(String to, String barberName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("barberName", barberName);

            String htmlContent = templateEngine.process("email/booking-notification-barber", context);

            sendEmail(
                to,
                "Nouvelle réservation - " + appName,
                htmlContent,
                true
            );

            log.info("Booking notification email sent to barber: {}", to);

        } catch (Exception e) {
            log.error("Failed to send booking notification email to barber: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer un email de mise à jour de réservation au client
     */
    public void sendBookingUpdateToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);

            String htmlContent = templateEngine.process("email/booking-update-client", context);

            sendEmail(
                to,
                "Mise à jour de votre réservation - " + appName,
                htmlContent,
                true
            );

            log.info("Booking update email sent to client: {}", to);

        } catch (Exception e) {
            log.error("Failed to send booking update email to client: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer un email d'annulation de réservation au client
     */
    public void sendBookingCancellationToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);

            String htmlContent = templateEngine.process("email/booking-cancellation-client", context);

            sendEmail(
                to,
                "Annulation de votre réservation - " + appName,
                htmlContent,
                true
            );

            log.info("Booking cancellation email sent to client: {}", to);

        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to client: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer un email d'annulation de réservation au barbier
     */
    public void sendBookingCancellationToBarber(String to, String barberName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("barberName", barberName);

            String htmlContent = templateEngine.process("email/booking-cancellation-barber", context);

            sendEmail(
                to,
                "Réservation annulée - " + appName,
                htmlContent,
                true
            );

            log.info("Booking cancellation email sent to barber: {}", to);

        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to barber: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer un rappel de réservation
     */
    public void sendBookingReminder(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);

            String htmlContent = templateEngine.process("email/booking-reminder", context);

            sendEmail(
                to,
                "Rappel de rendez-vous - " + appName,
                htmlContent,
                true
            );

            log.info("Booking reminder email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send booking reminder email: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer une demande d'avis
     */
    public void sendReviewRequest(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);

            String htmlContent = templateEngine.process("email/review-request", context);

            sendEmail(
                to,
                "Votre avis nous intéresse - " + appName,
                htmlContent,
                true
            );

            log.info("Review request email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send review request email: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoyer un email de bienvenue
     */
    public void sendWelcomeEmail(String to, String userName, String userType) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("userType", userType);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("email/welcome", context);

            sendEmail(
                to,
                "Bienvenue sur " + appName,
                htmlContent,
                true
            );

            log.info("Welcome email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage(), e);
        }
    }

    /**
     * Méthode générique pour envoyer un email
     */
    private void sendEmail(String to, String subject, String content, boolean isHtml) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, isHtml);

        mailSender.send(message);
    }
}
