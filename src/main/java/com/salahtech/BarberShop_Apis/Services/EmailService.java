package com.salahtech.BarberShop_Apis.Services;

import java.util.Map;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.repositories.DeviceTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.name:Barber Shop}")
    private String appName;

    // ========= Utils =========

    private boolean canSend() {
        if (!emailEnabled) {
            log.debug("Envoi d'emails désactivé (notification.email.enabled=false)");
            return false;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("fromEmail est vide -> email non envoyé");
            return false;
        }
        return true;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("Skip simple email: 'to' is null/blank");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email simple à {}: {}", to, e.getMessage(), e);
        }
    }

    private void sendEmail(String to, String subject, String content, boolean isHtml) throws MessagingException {
        if (to == null || to.isBlank()) {
            log.warn("Skip HTML email: 'to' is null/blank");
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, isHtml);
        mailSender.send(message);
    }

    // ========= Mails génériques =========

    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> model) {
        if (!canSend())
            return;
        try {
            if (to == null || to.isBlank()) {
                log.warn("Skip template email: 'to' is null/blank");
                return;
            }
            Context ctx = new Context();
            if (model != null && !model.isEmpty())
                ctx.setVariables(model);
            String tpl = (templateName != null && templateName.startsWith("email/"))
                    ? templateName
                    : "email/" + templateName;
            String html = templateEngine.process(tpl, ctx);
            sendEmail(to, subject, html, true);
            log.info("Template email '{}' envoyé à {}", tpl, to);
        } catch (Exception e) {
            log.error("Erreur envoi template '{}' à {}: {}", templateName, to, e.getMessage(), e);
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        if (!canSend())
            return;
        try {
            sendEmail(to, subject, text, false);
            log.info("Email texte envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur envoi email texte à {}: {}", to, e.getMessage(), e);
        }
    }

    // ========= Flots Auth =========

    @Async
    public void sendWelcomeEmail(ApplicationUser user) {
        if (!canSend())
            return;
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skip welcome email: user/email null");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue sur " + appName + " !");
            message.setText(String.format(
                    "Bonjour %s,\n\nBienvenue sur %s !\nVotre compte a été créé avec succès.\n\n" +
                            "Vous pouvez maintenant explorer nos services et prendre rendez-vous avec nos barbiers professionnels.\n\n"
                            +
                            "Cordialement,\nL'équipe %s",
                    user.getFirstName() != null ? user.getFirstName() : user.getEmail(), appName, appName));
            mailSender.send(message);
            log.info("Email de bienvenue envoyé à: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur envoi email de bienvenue à {}: {}", user != null ? user.getEmail() : "null",
                    e.getMessage(), e);
        }
    }

    /** NEW: envoi des identifiants au moment de la création par un admin */
    @Async
    public void sendAccountCredentialsEmail(ApplicationUser user, String plainPassword, boolean alreadyVerified) {
        if (!canSend())
            return;
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skip account credentials email: user/email null");
                return;
            }
            String loginUrl = frontendUrl + "/login"; // adapte si ton front est ailleurs
            String subject = alreadyVerified
                    ? "Vos identifiants de connexion - " + appName
                    : "Votre compte a été créé - " + appName;

            String html = String.format("""
                    <div style="font-family:Arial,sans-serif;line-height:1.5">
                      <h2 style="margin:0 0 12px">Bienvenue sur %s</h2>
                      <p>Bonjour %s,</p>
                      <p>Votre compte vient d’être créé. Voici vos identifiants de connexion :</p>
                      <ul>
                        <li><strong>Email :</strong> %s</li>
                        <li><strong>Mot de passe :</strong> %s</li>
                      </ul>
                      %s
                      <p>
                        <a href="%s" style="display:inline-block;padding:10px 16px;border-radius:6px;
                           background:#1976d2;color:#fff;text-decoration:none">Se connecter</a>
                      </p>
                      <p style="font-size:12px;color:#666">
                        Par sécurité, changez ce mot de passe après votre première connexion.
                      </p>
                      <p>Cordialement,<br>L’équipe %s</p>
                    </div>
                    """,
                    appName,
                    user.getFirstName() != null ? user.getFirstName() : user.getEmail(),
                    user.getEmail(),
                    plainPassword,
                    alreadyVerified
                            ? ""
                            : "<p>Un email de vérification vous a également été envoyé. Veuillez vérifier votre adresse email.</p>",
                    loginUrl,
                    appName);

            sendEmail(user.getEmail(), subject, html, true);
            log.info("Email d'identifiants envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec envoi email identifiants à {}: {}", user != null ? user.getEmail() : "null",
                    e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(ApplicationUser user) {
        if (!canSend())
            return;
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skip verification email: user/email null");
            return;
        }
        String subject = "Vérification de votre compte " + appName;
        String verificationUrl = frontendUrl + "/auth/verify-email?token=" + user.getVerificationToken();
        String message = String.format("""
                Bonjour %s,

                Merci de vous être inscrit sur %s !

                Pour activer votre compte, veuillez cliquer sur le lien ci-dessous :
                %s

                Si vous n'avez pas créé de compte, vous pouvez ignorer cet email.

                Cordialement,
                L'équipe %s
                """, user.getFirstName() != null ? user.getFirstName() : user.getEmail(), appName, verificationUrl,
                appName);
        sendSimpleMessage(user.getEmail(), subject, message);
    }

    @Async
    public void sendPasswordResetEmail(ApplicationUser user, String resetToken) {
        if (!canSend())
            return;
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skip reset email: user/email null");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Réinitialisation de votre mot de passe");
            // Lien vers la page web /reset-password (le token seul n'était saisissable nulle part).
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            message.setText(String.format(
                    "Bonjour %s,\n\nVous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                            "Choisissez un nouveau mot de passe en ouvrant ce lien (valable 1 heure) :\n%s\n\n" +
                            "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\nCordialement,\nL'équipe %s",
                    user.getFirstName() != null ? user.getFirstName() : user.getEmail(), resetUrl, appName));
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé à: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur envoi email de réinitialisation à {}: {}", user != null ? user.getEmail() : "null",
                    e.getMessage(), e);
        }
    }

    @Async
    public void sendPasswordChangedConfirmation(ApplicationUser user) {
        if (!canSend())
            return;
        try {
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Skip password changed email: user/email null");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Mot de passe modifié");
            message.setText(String.format(
                    "Bonjour %s,\n\nVotre mot de passe a été modifié avec succès.\n\n" +
                            "Si vous n'êtes pas à l'origine de cette modification, contactez-nous immédiatement.\n\n" +
                            "Cordialement,\nL'équipe %s",
                    user.getFirstName() != null ? user.getFirstName() : user.getEmail(), appName));
            mailSender.send(message);
            log.info("Email de confirmation de changement de mot de passe envoyé à: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erreur envoi email de confirmation à {}: {}", user != null ? user.getEmail() : "null",
                    e.getMessage(), e);
        }
    }

    // ========= Mails réservation (inchangé, sécurisés) =========

    public void sendBookingConfirmationToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);
            String htmlContent = templateEngine.process("email/booking-confirmation-client", context);
            sendEmail(to, "Confirmation de votre réservation - " + appName, htmlContent, true);
            log.info("Booking confirmation email sent to client: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to client {}: {}", to, e.getMessage(), e);
        }
    }

    public void deleteDeviceToken(Long userId) {
        try {
            deviceTokenRepository.deleteByUser_Id(userId); // méthode corrigée côté repo
            log.info("Device token deleted for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete device token for user {}: {}", userId, e.getMessage(), e);
        }
    }

    public void sendBookingNotificationToBarber(String to, String barberName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("barberName", barberName);
            String htmlContent = templateEngine.process("email/booking-notification-barber", context);
            sendEmail(to, "Nouvelle réservation - " + appName, htmlContent, true);
            log.info("Booking notification email sent to barber: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking notification email to barber {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendBookingUpdateToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);
            String htmlContent = templateEngine.process("email/booking-update-client", context);
            sendEmail(to, "Mise à jour de votre réservation - " + appName, htmlContent, true);
            log.info("Booking update email sent to client: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking update email to client {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendBookingCancellationToClient(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);
            String htmlContent = templateEngine.process("email/booking-cancellation-client", context);
            sendEmail(to, "Annulation de votre réservation - " + appName, htmlContent, true);
            log.info("Booking cancellation email sent to client: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to client {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendBookingCancellationToBarber(String to, String barberName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("barberName", barberName);
            String htmlContent = templateEngine.process("email/booking-cancellation-barber", context);
            sendEmail(to, "Réservation annulée - " + appName, htmlContent, true);
            log.info("Booking cancellation email sent to barber: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking cancellation email to barber {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendBookingReminder(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);
            String htmlContent = templateEngine.process("email/booking-reminder", context);
            sendEmail(to, "Rappel de rendez-vous - " + appName, htmlContent, true);
            log.info("Booking reminder email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send booking reminder email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendReviewRequest(String to, String clientName, Map<String, Object> templateData) {
        try {
            Context context = new Context();
            context.setVariables(templateData);
            context.setVariable("clientName", clientName);
            String htmlContent = templateEngine.process("email/review-request", context);
            sendEmail(to, "Votre avis nous intéresse - " + appName, htmlContent, true);
            log.info("Review request email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send review request email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmailHtml(String to, String userName, String userType) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("userType", userType);
            context.setVariable("appName", appName);
            String htmlContent = templateEngine.process("email/welcome", context);
            sendEmail(to, "Bienvenue sur " + appName, htmlContent, true);
            log.info("Welcome email (HTML) sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome HTML email to {}: {}", to, e.getMessage(), e);
        }
    }
}
