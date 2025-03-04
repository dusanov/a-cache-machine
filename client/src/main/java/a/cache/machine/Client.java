package a.cache.machine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Client implements IClient<String, String> {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    private String sendCommand(String... args) throws IOException {
        // Build the RESP command
        StringBuilder command = new StringBuilder();
        command.append("*").append(args.length).append("\r\n"); // Number of arguments
        for (String arg : args) {
            command.append("$").append(arg.length()).append("\r\n"); // Length of the argument
            command.append(arg).append("\r\n"); // The argument itself
        }
    
        // Send the command to the server
        writer.write(command.toString());
        writer.flush();
    
        // Read the response from the server
        String response = reader.readLine();
        if (response != null && response.startsWith("$")) {
            // Bulk string response (e.g., GET)
            int length = Integer.parseInt(response.substring(1));
            if (length == -1) {
                return null; // Null bulk string
            }
            String value = reader.readLine();
            return value;
        }
        return response;
    }

    public void close() throws IOException {
        reader.close();
        writer.close();
        socket.close();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 6379);

            // Example commands
            System.out.println("PING: " + client.sendCommand("PING")); // Should return "+PONG"
            System.out.println("SET: " + client.sendCommand("SET", "mykey", "myvalue")); // Should return "+OK"
            System.out.println("GET: " + client.sendCommand("GET", "mykey")); // Should return "$7\r\nmyvalue"
            System.out.println("DEL: " + client.sendCommand("DEL", "mykey")); // Should return ":1"

            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String ping() throws Exception{
        return sendCommand("PING");
    }

    @Override
    public String put(String key, String value) throws Exception {
       return sendCommand("SET",key,value);
    }

    @Override
    public String get(String key) throws Exception {
        return sendCommand("GET",key);
    }

    @Override
    public String remove(String key) throws Exception {
        return sendCommand("DEL",key);
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }
}