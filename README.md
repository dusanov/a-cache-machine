# a-cache-machine
exercise in java caching

### Testing the Server

1. Start the server:
   ```
   ./gradlew run
   ```

2. Use `telnet` to connect to the server:
   ```
   telnet localhost 6379
   ```

3. Send RESP commands:

   - **PING**:
     ```
     *1\r\n$4\r\nPING\r\n
     ```
     Response:
     ```
     +PONG\r\n
     ```

   - **SET**:
     ```
     *3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n
     ```
     Response:
     ```
     +OK\r\n
     ```

   - **GET**:
     ```
     *2\r\n$3\r\nGET\r\n$3\r\nkey\r\n
     ```
     Response:
     ```
     $5\r\nvalue\r\n
     ```

   - **DEL**:
     ```
     *3\r\n$3\r\nDEL\r\n$3\r\nkey\r\n$6\r\nkey2\r\n
     ```
     Response:
     ```
     :1\r\n
     ```