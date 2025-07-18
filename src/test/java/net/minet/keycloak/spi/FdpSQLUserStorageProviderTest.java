package net.minet.keycloak.spi;

import net.minet.keycloak.hash.Md4Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FdpSQLUserStorageProviderTest {

    @BeforeEach
    public void setup() throws Exception {
        
    }

    @Test
    public void testMD4hash() {
        assertEquals("c79fd641cfa02d5fa374284887ae53a0", Md4Util.md4Hex("1234zefekghn"));
    }
}
