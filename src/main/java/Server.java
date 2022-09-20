import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Server {
    final ExecutorService executorService = Executors.newFixedThreadPool(64);

    private final Map<String, Map<String, Handler>> handlers;


    public Server() {
        this.handlers = new ConcurrentHashMap<>();
    }


    public void start(int port) {
        System.out.println("Сервер начал выполнение");
        while (true) {
            try (final var serverSocket = new ServerSocket(port)) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> {
                    parseRequest(socket);
                });
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void addHandler(String method, String message, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<String, Handler>());
            handlers.get(method).put(message, handler);
        } else {
            handlers.get(method).put(message, handler);
        }
        System.out.println("Добавлен метод " + method + " адрес: " + message);

    }


    public void parseRequest(Socket socket) {
        try (final var out = new BufferedOutputStream(socket.getOutputStream())) {
            final Request request = Request.parse(socket);

            if (handlers.containsKey(request.getMethod()) && handlers.get(request.getMethod()).containsKey(request.getPath())) {
                handlers.get(request.getMethod()).get(request.getPath()).handle(request, out);
            } else {
                Request.badRequest(out);
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
    }
}
