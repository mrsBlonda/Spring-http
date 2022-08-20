import org.apache.http.NameValuePair;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Request {
    private String method;
    private String body;
    private List<String> headers;
    private long length;
    private String path;

    private List<NameValuePair> queryParams;

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
