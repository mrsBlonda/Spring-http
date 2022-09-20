import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {
    private String method;
    private String body;
    private List<String> headers;
    private long length;
    private String path;

    private List<NameValuePair> queryParams;

    private static final List<String> allowedMethods = List.of("GET", "POST");

    public Request(String method, String body, String path, List<String> headers, List<NameValuePair> queryParams) {
        this.method = method;
        this.body = body;
        this.headers = headers;
        this.path = path;
        this.queryParams = queryParams;
    }

    public static void successfulRequest(Request request, BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + request.getType() + "\r\n" +
                        "Content-Length: " + request.getLength() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());

    }
    public static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
    public static void timeRequest(Request request, BufferedOutputStream out) throws IOException {
        var filePath = Path.of(".", "public", request.getPath());
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + request.getType() + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    public static Request parse(Socket socket) throws IOException, URISyntaxException {
        final var in = new BufferedInputStream(socket.getInputStream());
        final var out = new BufferedOutputStream(socket.getOutputStream());

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

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
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
    public String getType() throws IOException {
        var filePath = Path.of(".", "public", getPath());
        var mimeType = Files.probeContentType(filePath);
        return mimeType;
    }
    public String getMethod() {
        return method;
    }

    public long getLength() throws IOException {
        var filePath = Path.of(".", "public", getPath());
        length = Files.size(filePath);
        return length;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
    public List<NameValuePair> getQueryParam(String name) {
        List<NameValuePair> queryParam = null;
        for (NameValuePair query : queryParams) {
            if (query.getName().equals(name)) 
                queryParam.add(query);
        }
        return queryParam;

    }
}
