import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Server {
    final ExecutorService executorService = Executors.newFixedThreadPool(64);
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final Map<String, Map<String, Handler>> handlers;
    final List<String> allowedMethods = List.of("GET", "POST");

    public Server() {
        this.handlers = new ConcurrentHashMap<>();
    }




    public void start(int port) {
        System.out.println("Сервер начал выполнение");

        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {

                final var socket = serverSocket.accept();

                executorService.execute(() -> {
                    try {

                        answer(socket);

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

    public void successfulRequest(Request request, BufferedOutputStream out) throws IOException {

        var filePath = Path.of(".", "public", request.getPath());
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + request.getType() + "\r\n" +
                        "Content-Length: " + request.getLength() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }




    public void addHandler(String method, String message, Handler handler) {
        if(!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<String, Handler>());
            handlers.get(method).put(message, handler);
        } else {
            handlers.get(method).put(message, handler);
        }
        System.out.println("Добавлен метод " + method + " адрес: " + message);

    }

    public Request parse(Socket socket, BufferedInputStream in, BufferedOutputStream out) throws IOException {


        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
            socket.close();
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
            socket.close();
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
            socket.close();
        }
        System.out.println(method);

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(out);
            socket.close();
        }
        System.out.println(path);

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            socket.close();
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        // для GET тела нет
        String body = "null";
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                System.out.println(body);
            }
        }

        return new Request(method, body, path, headers);
    }

    public void answer(Socket socket) throws IOException {
        final var in = new BufferedInputStream(socket.getInputStream());
        final var out = new BufferedOutputStream(socket.getOutputStream());
        Request request = parse(socket, in, out);
        if (handlers.containsKey(request.method) && handlers.get(request.method).containsKey(request.path)) {
            successfulRequest(request, out);
        } else {
            badRequest(out);
        }
    }
    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }


}
