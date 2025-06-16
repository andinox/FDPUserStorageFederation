package net.minet.keycloak.spi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "adherents")
public class ExternalUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom")
    private String lastName;

    @Column(name = "prenom")
    private String firstName;

    @Column(name = "mail")
    private String email;

    @Column(name = "login", unique = true)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "date_de_depart")
    private LocalDate departureDate;

    @Column(name = "commentaires")
    private String comments;

    @Column(name = "mode_association")
    private Byte modeAssociation;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "subnet")
    private String subnet;

    @Column(name = "ip")
    private String ip;

    @Column(name = "chambre_id")
    private Integer chambreId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "edminet")
    private Byte edminet;

    @Column(name = "is_naina")
    private Byte isNaina;

    @Column(name = "mailinglist")
    private Byte mailingList;

    @Column(name = "mail_membership")
    private Integer mailMembership;

    @Column(name = "ldap_login")
    private String ldapLogin;

    @Column(name = "datesignedhosting")
    private LocalDateTime dateSignedHosting;

    @Column(name = "datesignedadhesion")
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

