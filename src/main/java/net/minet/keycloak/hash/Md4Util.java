package net.minet.keycloak.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.HexFormat;

/** Utility for computing MD4 digests as hexadecimal strings. */
public final class Md4Util {

    /**
     * Bloc statique qui vérifie si le fournisseur BouncyCastle est déjà enregistré.
     * Si ce n'est pas le cas, il l'ajoute au registre des fournisseurs de sécurité.
     */
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Calcule un hachage MD4 en UTF-16LE. Utilisé par Keycloak pour comparer les mots de passe d'anciens systèmes.
     */
    public static String md4Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD4");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_16LE));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD4 algorithm not available", e);
        }
    }
}
