import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Request {

    public String method;
    public String body;
    public List<String> headers;
    private long length;
    public String path;

    public Request(String method, String body, String path, List<String> headers) {
        this.method = method;
        this.body = body;
        this.headers = headers;

        this.path = path;
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
}
