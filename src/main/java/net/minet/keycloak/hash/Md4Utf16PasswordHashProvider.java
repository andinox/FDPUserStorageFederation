package net.minet.keycloak.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.credential.PasswordCredentialModel;

public class Md4Utf16PasswordHashProvider implements PasswordHashProvider {
    public static final String ID = "md4-utf16";
    private final int defaultIterations;

    public Md4Utf16PasswordHashProvider(int defaultIterations) {
        this.defaultIterations = defaultIterations;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, PasswordCredentialModel credential) {
        int iter = policy.getHashIterations();
        if (iter == -1) iter = defaultIterations;
        return credential.getPasswordCredentialData().getHashIterations() == iter
                && ID.equals(credential.getPasswordCredentialData().getAlgorithm());
    }

    @Override
    public PasswordCredentialModel encodedCredential(String rawPassword, int iterations) {
        if (iterations == -1) iterations = defaultIterations;
        String hash = encode(rawPassword, iterations);
        return PasswordCredentialModel.createFromValues(ID, new byte[0], iterations, hash);
    }

    @Override
    public String encode(String rawPassword, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD4");
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_16LE));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD4 algorithm not available", e);
        }
    }

    @Override
    public boolean verify(String rawPassword, PasswordCredentialModel credential) {
        String hash = encode(rawPassword, credential.getPasswordCredentialData().getHashIterations());
        return hash.equalsIgnoreCase(credential.getPasswordSecretData().getValue());
    }

    @Override
    public void close() {
    }
}
