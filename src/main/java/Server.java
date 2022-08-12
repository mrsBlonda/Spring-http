import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;


public class Server {
    final ExecutorService executorService = Executors.newFixedThreadPool(64);
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    final HashMap<String, HashMap<String, Handler>> handlers = null;



    public void start(int port) {
        System.out.println("Сервер начал выполнение");

        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {

                final var socket = serverSocket.accept();
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
                executorService.execute(() -> {
                    try {
                        connect(socket, in, out);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });


            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void timeRequest(Path filePath, BufferedOutputStream out, String mimeType) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    public void successfulRequest(Path filePath, BufferedOutputStream out, String mimeType) throws IOException {
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public void connect(Socket socket, BufferedReader in, BufferedOutputStream out) throws IOException {


        final var requestLine = in.readLine();
        System.out.println("Результат работы потока " + Thread.currentThread().getName() + " : " + requestLine);
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            socket.close();
            // just close socket
        }

        final var path = parts[1];
        if (!validPaths.contains(path)) {
            badRequest(out);
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            timeRequest(filePath, out, mimeType);
        }
        successfulRequest(filePath, out, mimeType);



    }


    public void addHandler(String method, String message, Handler handler) {
        if(!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<String, Handler>());
            handlers.get(method).put(message, handler);
        } else {
            handlers.get(method).put(message, handler);
        }

    }


}
