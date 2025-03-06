package a.cache.machine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import a.cache.machine.engine.ICache;

public class RESPServer {
    private static final Logger logger = LoggerFactory.getLogger(RESPServer.class);
    private static boolean running = true;
    private final int PORT;
    private final ICache<String, Object> cache;

    public static String getGreeting() {
        return "RESP Server is listening on port ";
    }

    public RESPServer(ICache<String, Object> cache, int port) {
        this.cache = cache;
        this.PORT = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info(getGreeting() + PORT);
            while (running) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected");
                new ClientHandler(socket, cache).start();
            }
        } catch (IOException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
        }
        logger.info(".... Server stopped ....");        
    }

    public static void shutdown() {
        logger.info(".... Shutting down server ....");
        running = false;
    }
}
