import net.minet.keycloak.hash.Md4Utf16PasswordHashProvider;
import org.junit.jupiter.api.Test;
import org.keycloak.models.credential.PasswordCredentialModel;

import static org.junit.jupiter.api.Assertions.*;

public class Md4Utf16PasswordHashProviderTest {

    @Test
    public void testEncodeAndVerify() {
        Md4Utf16PasswordHashProvider provider = new Md4Utf16PasswordHashProvider(1);
        String raw = "password";
        String hash = provider.encode(raw, 1);
        assertEquals("8846f7eaee8fb117ad06bdd830b7586c", hash);

        PasswordCredentialModel cred = provider.encodedCredential(raw, 1);
        assertTrue(provider.verify(raw, cred));
        assertFalse(provider.verify("wrong", cred));
    }
}
