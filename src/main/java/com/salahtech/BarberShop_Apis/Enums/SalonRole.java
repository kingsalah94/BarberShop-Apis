package com.salahtech.BarberShop_Apis.Enums;

/**
 * Rôle d'accès d'un utilisateur SUR un salon (membership), distinct du
 * {@link ApplicationUserType} global. Un même salon a un OWNER et,
 * éventuellement, des sous-comptes MANAGER / RECEPTIONIST.
 */
public enum SalonRole {
    OWNER,
    MANAGER,
    RECEPTIONIST,
    /** Prestataire (barbier/coiffeur) : accès à son propre espace pro. */
    EMPLOYEE
}
