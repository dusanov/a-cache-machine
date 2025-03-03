package a.cache.machine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;

public class ClientTest {

    private Client client;

    @BeforeEach
    public void setUp() throws IOException {
        // Initialize the client to connect to a test server (e.g., localhost:6379)
        client = new Client("localhost", 6379);
    }

    @Test
    public void testPing() throws IOException {
        // Test the PING command
        String response = client.sendCommand("PING");
        assertEquals("+PONG", response, "PING command should return +PONG");
    }

    @Test
    public void testSetAndGet() throws IOException {
        // Test the SET command
        String setResponse = client.sendCommand("SET", "testKey", "testValue");
        assertEquals("+OK", setResponse, "SET command should return +OK");

        // Test the GET command
        String getResponse = client.sendCommand("GET", "testKey");
        assertEquals("testValue", getResponse, "GET command should return the value set by SET");
    }

    @Test
    public void testGetNonExistentKey() throws IOException {
        // Test GET for a key that does not exist
        String response = client.sendCommand("GET", "nonExistentKey");
        assertNull(response, "GET command should return null for a non-existent key");
    }

    @Test
    public void testDel() throws IOException {
        // Set a key to delete
        client.sendCommand("SET", "keyToDelete", "value");

        // Test the DEL command
        String delResponse = client.sendCommand("DEL", "keyToDelete");
        assertEquals(":1", delResponse, "DEL command should return :1 for a successful deletion");

        // Verify the key is deleted
        String getResponse = client.sendCommand("GET", "keyToDelete");
        assertNull(getResponse, "GET command should return null after deleting the key");
    }

    @Test
    public void testDelNonExistentKey() throws IOException {
        // Test DEL for a key that does not exist
        String response = client.sendCommand("DEL", "nonExistentKey");
        assertEquals(":0", response, "DEL command should return :0 for a non-existent key");
    }

    @Test
    public void testInvalidCommand() throws IOException {
        // Test an invalid command
        String response = client.sendCommand("INVALID");
        assertTrue(response.startsWith("-ERR"), "Invalid command should return an error response");
    }

    @Test
    public void testClose() throws IOException {
        // Test closing the client
        client.close();
        assertThrows(IOException.class, () -> client.sendCommand("PING"), "Sending a command after closing the client should throw an IOException");
    }
}