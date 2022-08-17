import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + request.getType() + "\r\n" +
                            "Content-Length: " + request.getLength() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(Path.of(".", "public", request.getPath()), responseStream);
            responseStream.flush();

        });

        server.addHandler("POST", "message", (request, responseStream) -> {
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + request.getType() + "\r\n" +
                            "Content-Length: " + request.getLength() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(Path.of(".", "public", request.getPath()), responseStream);
            responseStream.flush();

        });




        server.start(9999);




    }
}

