package a.cache.machine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import a.cache.machine.engine.ICache;
import a.cache.machine.engine.LRUCache;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final int PORT = 6379;
    private static int MAX_SIZE_IN_BYTES = 100;

    public static String getGreeting() {
        return "RESP Server is listening on port ";
    }

    public static void main(String[] args) {
        ICache<String, Object> cache = new LRUCache<>(MAX_SIZE_IN_BYTES);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info(getGreeting() + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("New client connected");

                new ClientHandler(socket, cache).start();
            }
        } catch (IOException ex) {
            logger.error(ex.getLocalizedMessage());
        }
    }
}
