package net.minet.keycloak.spi.entity;

import java.time.LocalDateTime;

/**
 * Simple data object representing a user record in the external SQL database.
 */
public class ExternalUser {
    private Integer id;
    private String lastName;
    private String firstName;
    private String email;
    private String username;
    private LocalDateTime createdAt;
    private Byte isNaina;
    private String ldapLogin;

    /** Identifiant unique utilisé par Keycloak pour faire le lien avec la base externe. */
    public Integer getId() {
        return id;
    }

    /** Fixe l'identifiant lors du chargement ou de la création de l'utilisateur. */
    public void setId(Integer id) {
        this.id = id;
    }

    /** Nom de famille renvoyé à Keycloak. */
    public String getLastName() {
        return lastName;
    }

    /** Mise à jour du nom de famille. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** Prénom associé à l'utilisateur. */
    public String getFirstName() {
        return firstName;
    }

    /** Modifie le prénom enregistré. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** Adresse email de référence pour Keycloak. */
    public String getEmail() {
        return email;
    }

    /** Définit l'adresse email. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Login utilisé pour l'authentification. */
    public String getUsername() {
        return username;
    }

    /** Change le login de l'utilisateur. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** Date de création originale de l'utilisateur. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Positionne la date de création. */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Flag spécifique à l'application externe. */
    public Byte getIsNaina() {
        return isNaina;
    }

    public void setIsNaina(Byte isNaina) {
        this.isNaina = isNaina;
    }

    /** Valeur de login LDAP associée le cas échéant. */
    public String getLdapLogin() {
        return ldapLogin;
    }

    /** Modifie le login LDAP. */
    public void setLdapLogin(String ldapLogin) {
        this.ldapLogin = ldapLogin;
    }
}
