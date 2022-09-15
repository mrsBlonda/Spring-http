import java.nio.file.Files;
import java.nio.file.Path;

public class Main {


    public static void main(String[] args) {
        Server server = new Server();
        server.addHandler("GET", "/index.html", (request, responseStream) -> {
            Request.successfulRequest(request, responseStream);
            Files.copy(Path.of(".", "public", request.getPath()), responseStream);
            responseStream.flush();

        });

        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            Request.timeRequest(request, responseStream);
            Files.copy(Path.of(".", "public", request.getPath()), responseStream);
            responseStream.flush();

        });

        server.addHandler("POST", "message", (request, responseStream) -> {
            Request.successfulRequest(request, responseStream);
            Files.copy(Path.of(".", "public", request.getPath()), responseStream);
            responseStream.flush();

        });

        server.start(9999);
    }
}

