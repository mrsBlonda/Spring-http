import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Request {

    public String method;
    public String body;
    public List<String> headers;

    private String type;
    private int length;
    public String path;

    public Request(String method, String body, String path, List<String> headers) {
        this.method = method;
        this.body = body;
        this.headers = headers;

        this.path = path;
    }


    public String getType() {
        return type;
    }

    public int getLength() throws IOException {
        var filePath = Path.of(".", "public", getPath());
        Files.size(filePath);
        return length;
    }

    public String getPath() {
        return path;
    }
}
