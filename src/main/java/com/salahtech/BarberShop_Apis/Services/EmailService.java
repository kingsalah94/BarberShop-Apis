package com.salahtech.BarberShop_Apis.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.enabled}")
    private boolean emailEnabled;

    @Async
    public void sendWelcomeEmail(com.salahtech.BarberShop_Apis.models.ApplicationUser user) {
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
}
