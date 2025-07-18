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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Byte getIsNaina() {
        return isNaina;
    }

    public void setIsNaina(Byte isNaina) {
        this.isNaina = isNaina;
    }

    public String getLdapLogin() {
        return ldapLogin;
    }

    public void setLdapLogin(String ldapLogin) {
        this.ldapLogin = ldapLogin;
    }
}
