package net.minet.keycloak.hash;

import net.minet.keycloak.hash.Md4Util;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * Fournisseur de hachage de mot de passe qui calcule un digest MD4 du mot de passe encodé en UTF-16.
 * Utilisé pour vérifier les mots de passe provenant de systèmes hérités.
 */
public class Md4Utf16PasswordHashProvider implements PasswordHashProvider {
    /** Identifiant unique pour ce fournisseur de hachage de mot de passe. */
    public static final String ID = "md4-utf16";
    private final int defaultIterations;

    /**
     * Constructeur de la classe.
     *
     * @param defaultIterations Le nombre d'itérations par défaut pour le hachage.
     */
    public Md4Utf16PasswordHashProvider(int defaultIterations) {
        this.defaultIterations = defaultIterations;
    }

    /**
     * Vérifie si les informations d'identification respectent la politique de mot de passe.
     *
     * @param policy     La politique de mot de passe à vérifier.
     * @param credential Les informations d'identification du mot de passe.
     * @return  si la politique est respectée, sinon `false`.
     */
    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        int iter = policy.getHashIterations();
        if (iter == -1) iter = defaultIterations;
        return credential.getPasswordCredentialData().getHashIterations() == iter
                && ID.equals(credential.getPasswordCredentialData().getAlgorithm());
    }

    /**
     * Crée un modèle d'informations d'identification encodé pour un mot de passe brut.
     *
     * @param rawPassword Le mot de passe brut.
     * @param iterations  Le nombre d'itérations pour le hachage.
     * @return Un modèle d'informations d'identification encodé.
     */
    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        if (iterations == -1) iterations = defaultIterations;
        String hash = encode(rawPassword, iterations);
        return PasswordCredentialModel.createFromValues(ID, new byte[0], iterations, hash);
    }

    /**
     * Encode un mot de passe brut en utilisant l'algorithme MD4.
     *
     * @param rawPassword Le mot de passe brut.
     * @param iterations  Le nombre d'itérations pour le hachage (non utilisé dans MD4).
     * @return Le mot de passe encodé sous forme de chaîne hexadécimale.
     */
    @Override
    public String encode(String rawPassword, int iterations) {
        return Md4Util.md4Hex(rawPassword);
    }

    /**
     * Vérifie si un mot de passe brut correspond à un mot de passe encodé.
     *
     * @param rawPassword Le mot de passe brut.
     * @param credential  Les informations d'identification encodées.
     * @return `true` si le mot de passe correspond, sinon `false`.
     */
    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        String hash = encode(rawPassword, credential.getPasswordCredentialData().getHashIterations());
        return hash.equalsIgnoreCase(credential.getPasswordSecretData().getValue());
    }

    /**
     * Ferme le fournisseur et libère les ressources associées.
     */
    @Override
    public void close() {
    }
}