package a.cache.machine.engine.listener;

import a.cache.machine.engine.ICacheEventListener;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/cache-events")
public class SocketListener implements ICacheEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SimpleCacheEventLogger.class);
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Client connected " + session.getId());
        clients.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("Client disconnected " + session.getId());
        clients.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("Received message from client " + session.getId() + ": " + message);
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (Session client : clients) {
                try {
                    client.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage(),e);
                }
            }
        }
    }

    @Override
    public void onHit(String key) {
        String message = "Cache hit for key: " + key;
        broadcast(message);
    }

    @Override
    public void onMiss(String key) {
        String message = "Cache miss for key: " + key;
        broadcast(message);
    }

    @Override
    public void onEviction(String key, Object value) {
        String message = "Eviction for key: " + key + ", value: " + value;
        broadcast(message);
    }
}
