package a.cache.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class ClientTest {

    private Client client;

    @BeforeEach
    public void setUp() throws Exception {
        client = new Client("localhost", 6379);
    }

    @Test
    public void testPing() throws Exception {
        String response = client.ping();
        assertEquals("+PONG", response, "PING command should return +PONG");
    }

    @Test
    public void testSetAndGet() throws Exception {
        String setResponse = client.put("testKey", "testValue");
        assertEquals("+OK", setResponse, "SET command should return +OK");

        String getResponse = client.get("testKey");
        assertEquals("testValue", getResponse, "GET command should return the value set by SET");
    }

    @Test
    public void testGetNonExistentKey() throws Exception {
        String response = client.get( "nonExistentKey");
        assertNull(response, "GET command should return null for a non-existent key");
    }

    @Test
    public void testDel() throws Exception {
        // Set a key to delete
        client.put( "keyToDelete", "value");

        String delResponse = client.remove("keyToDelete");
        assertEquals(":1", delResponse, "DEL command should return :1 for a successful deletion");

        // Verify the key is deleted
        String getResponse = client.get("keyToDelete");
        assertNull(getResponse, "GET command should return null after deleting the key");
    }

    @Test
    public void testDelNonExistentKey() throws Exception {
        String response = client.remove( "nonExistentKey");
        assertEquals(":0", response, "DEL command should return :0 for a non-existent key");
    }
}