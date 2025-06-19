package net.minet.keycloak.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Md4UtilTest {
    @Test
    void computesMd4HexCorrectly() {
        assertEquals("8846f7eaee8fb117ad06bdd830b7586c", Md4Util.md4Hex("password"));
        assertEquals("209c6174da490caeb422f3fa5a7ae634", Md4Util.md4Hex("admin"));
        assertEquals("31d6cfe0d16ae931b73c59d7e0c089c0", Md4Util.md4Hex(""));
    }
}
