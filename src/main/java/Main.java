import java.io.BufferedOutputStream;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();
        server.start(9999);

        server.addHandler("GET", "/messages", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) {

            }
        });


    }
}

