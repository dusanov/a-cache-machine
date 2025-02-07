package a.cache.machine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import a.cache.machine.engine.CacheException;
import a.cache.machine.engine.ICache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClientHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private Socket socket;
    private ICache<String, Object> dataStore; // In-memory data store

    public ClientHandler(Socket socket, ICache<String, Object> cache) {
        this.socket = socket;
        this.dataStore = cache;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

            while (true) {
                String respLine = reader.readLine();
                if (respLine == null) {
                    break; // Client disconnected
                }

                logger.info("Received: " + respLine);

                // Parse the RESP command
                if (respLine.startsWith("*")) {
                    String[] splitRespLine = respLine.split("\\\\r\\\\n");
                    int numArgs = Integer.parseInt(splitRespLine[0].substring(1));
                    //*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n
                    String[] args = new String[numArgs];
                    for (int i = 0; i < numArgs; i++) {
                        args[i] = splitRespLine[2 * i + 2];
                    }

                    // Process the command
                    String response = processCommand(args);

                    // Send RESP response
                    writer.write(response);
                    writer.flush();
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getLocalizedMessage());
        // } catch (CacheException e) {
        //     logger.error(e.getLocalizedMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.error(ex.getLocalizedMessage());
            }
        }
    }

    private String processCommand(String[] args) /* throws CacheException */ {
        if (args.length == 0) {
            return "-ERR No command provided\r\n";
        }

        String command = args[0].toUpperCase();

        switch (command) {
            case "PING":
                return "+PONG\r\n";

            case "SET":
                if (args.length < 3) {
                    return "-ERR Wrong number of arguments for 'SET'\r\n";
                }
                try {
                    dataStore.put(args[1], args[2]);
                } catch (CacheException e) {
                    return "-ERR " + e.getLocalizedMessage() + "\r\n";
                }
                return "+OK\r\n";

            case "GET":
                if (args.length < 2) {
                    return "-ERR Wrong number of arguments for 'GET'\r\n";
                }
                Object value;
                try {
                    value = dataStore.get(args[1]);
                } catch (CacheException e) {
                    return "-ERR " + e.getLocalizedMessage() + "\r\n";
                }
                if (value == null) {
                    return "$-1\r\n"; // Null bulk string
                }
                return "$" + ((String) value).length() + "\r\n" + value + "\r\n";

            case "DEL":
                if (args.length < 2) {
                    return "-ERR Wrong number of arguments for 'DEL'\r\n";
                }
                int deletedKeys = 0;
                for (int i = 1; i < args.length; i++) {
                    try {
                        if (dataStore.remove(args[i]) != null) {
                            deletedKeys++;
                        }
                    } catch (CacheException e) {
                        return "-ERR " + e.getLocalizedMessage() + "\r\n";
                    }
                }
                return ":" + deletedKeys + "\r\n"; // Integer response

            default:
                return "-ERR Unknown command '" + command + "'\r\n";
        }
    }
}