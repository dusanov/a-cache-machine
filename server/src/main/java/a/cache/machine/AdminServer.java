package a.cache.machine;

import com.sun.net.httpserver.HttpServer;

import a.cache.machine.engine.ICache;

import com.sun.net.httpserver.HttpHandler;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminServer {
    private static final Logger logger = LoggerFactory.getLogger(AdminServer.class);
    private final int PORT;
    private final ICache<String, Object> cache;

    public AdminServer(ICache<String, Object> cache, int port) {
        this.cache = cache;
        this.PORT = port;
    }

    public void start() {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/admin", new TemplateHandler(cache));
            server.setExecutor(null);
            server.start();
            logger.info("HTTP server is running on port " + PORT);
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }
}

class TemplateHandler implements HttpHandler {
    private final PebbleEngine engine;
    private final ICache<String, Object> cache;
    private static final Logger logger = LoggerFactory.getLogger(AdminServer.class);

    public TemplateHandler(ICache<String, Object> cache) {
        this.engine = new PebbleEngine.Builder().build();
        this.cache = cache;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logger.info("Handling request");
        PebbleTemplate compiledTemplate = engine.getTemplate("templates/index.peb");

        Map<String, Object> context = new HashMap<>();
        context.put("message", "Cache metrics");
        context.put("hits", cache.getMetrics().getHits());
        context.put("misses", cache.getMetrics().getMisses());
        context.put("evictions", cache.getMetrics().getEvictions());

        StringWriter writer = new StringWriter();
        compiledTemplate.evaluate(writer, context);

        String response = writer.toString();
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}