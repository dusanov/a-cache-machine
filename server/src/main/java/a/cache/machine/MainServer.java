package a.cache.machine;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import a.cache.machine.engine.ICache;
import a.cache.machine.engine.ICacheEventListener;
import a.cache.machine.engine.LRUCache;
public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);
    public static void main(String[] args) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Properties appProps = new Properties();
        try (InputStream resourceStream = classLoader.getResourceAsStream("server.properties")) {
            appProps.load(resourceStream);
            int maxCacheSizeInBytes = Integer.parseInt(appProps.getProperty("server.maxSizeInBytes"));
            int port = Integer.parseInt(appProps.getProperty("server.port"));
            int adminPort = Integer.parseInt(appProps.getProperty("server.admin.port"));
            ICache<String, Object> cache = new LRUCache<>(maxCacheSizeInBytes);

            try {
                String listeners[] = appProps.getProperty("server.listeners").split(",");
                for (String listener : listeners) {
                    Class<?> listenerClass = Class.forName(listener);
                    Object listenerInstance = listenerClass.getConstructor().newInstance();
                    if (listenerInstance instanceof ICacheEventListener) {
                        cache.addEventListener((ICacheEventListener)listenerInstance);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load listeners: " + e.getLocalizedMessage(), e);
            }

            Thread respServerThread = new Thread(() -> {
                RESPServer respServer = new RESPServer(cache, port);
                respServer.start();
            });

            Thread adminServerThread = new Thread(() -> {
                AdminServer adminServer = new AdminServer(cache, adminPort);
                adminServer.start();
            });

            respServerThread.start();
            adminServerThread.start();

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(),e);
        }            
    }
}