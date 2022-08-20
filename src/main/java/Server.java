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
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public Request parse(Socket socket, BufferedInputStream in, BufferedOutputStream out) throws IOException, URISyntaxException {


        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            Request.badRequest(out);
            socket.close();
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            Request.badRequest(out);
            socket.close();
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            Request.badRequest(out);
            socket.close();
        }
        System.out.println(method);

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            Request.badRequest(out);
            socket.close();
        }
        System.out.println(path);

        URIBuilder uriBuilder = new URIBuilder(path);
        var queryParams = uriBuilder.getQueryParams();
        for (NameValuePair query : queryParams) {
            System.out.println("Query параметр: " + query);
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            Request.badRequest(out);
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
        return new Request(method, body, path, headers, queryParams);
    }

    public void answer(Socket socket) throws IOException, URISyntaxException {
        final var in = new BufferedInputStream(socket.getInputStream());
        final var out = new BufferedOutputStream(socket.getOutputStream());
        Request request = parse(socket, in, out);
        if (handlers.containsKey(request.getMethod()) && handlers.get(request.getMethod()).containsKey(request.getPath())) {
            handlers.get(request.getMethod()).get(request.getPath()).handle(request, out);
        } else {
            Request.badRequest(out);

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
