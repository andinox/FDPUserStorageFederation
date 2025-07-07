package net.minet.keycloak.spi.entity;

import java.time.LocalDate;
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
    private String password;
    private LocalDate departureDate;
    private String comments;
    private Byte modeAssociation;
    private String accessToken;
    private String subnet;
    private String ip;
    private Integer chambreId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Byte edminet;
    private Byte isNaina;
    private Byte mailingList;
    private Integer mailMembership;
    private String ldapLogin;
    private LocalDateTime dateSignedHosting;
    private LocalDateTime dateSignedAdhesion;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Byte getModeAssociation() {
        return modeAssociation;
    }

    public void setModeAssociation(Byte modeAssociation) {
        this.modeAssociation = modeAssociation;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getChambreId() {
        return chambreId;
    }

    public void setChambreId(Integer chambreId) {
        this.chambreId = chambreId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Byte getEdminet() {
        return edminet;
    }

    public void setEdminet(Byte edminet) {
        this.edminet = edminet;
    }

    public Byte getIsNaina() {
        return isNaina;
    }

    public void setIsNaina(Byte isNaina) {
        this.isNaina = isNaina;
    }

    public Byte getMailingList() {
        return mailingList;
    }

    public void setMailingList(Byte mailingList) {
        this.mailingList = mailingList;
    }

    public Integer getMailMembership() {
        return mailMembership;
    }

    public void setMailMembership(Integer mailMembership) {
        this.mailMembership = mailMembership;
    }

    public String getLdapLogin() {
        return ldapLogin;
    }

    public void setLdapLogin(String ldapLogin) {
        this.ldapLogin = ldapLogin;
    }


    public LocalDateTime getDateSignedHosting() {
        return dateSignedHosting;
    }

    public void setDateSignedHosting(LocalDateTime dateSignedHosting) {
        this.dateSignedHosting = dateSignedHosting;
    }

    public LocalDateTime getDateSignedAdhesion() {
        return dateSignedAdhesion;
    }

    public void setDateSignedAdhesion(LocalDateTime dateSignedAdhesion) {
        this.dateSignedAdhesion = dateSignedAdhesion;
    }
}

