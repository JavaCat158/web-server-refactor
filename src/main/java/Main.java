import java.io.BufferedOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class Main {
    private static final int SERVER_PORT = 9999;
    private static final int THREAD_POOL_SIZE = 64;

    public static void main(String[] args) {
        Server server = new Server(SERVER_PORT, THREAD_POOL_SIZE);


        // добавление handler'ов (обработчиков)
        server.addHandler("POST", "/message", (request, responseStream) -> {
            try {
                server.responseWithContent(responseStream, "404", "Not Found!!!","Not Available".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        server.addHandler("GET", "/messages", (request, responseStream) ->
                server.responseWithContent(responseStream, "503", "Service Unavailable", "NO!".getBytes()));

        server.addHandler("GET", "/message", ((request, responseStream) ->
                server.defaultHandler(responseStream, "classic.html")));

        server.addHandler("POST", "/message", ((request, outputStream) ->
                server.defaultHandler(outputStream, "index.html")));

        server.addHandler("GET", "/form.html", (request, responseStream) -> {
            String query = request.getQueryParams().toString();
            server.defaultHandler(responseStream, "/form.html");
            System.out.println(query);
        });

        server.addHandler("GET", "/index.html", (request, out) -> {
           var path = Path.of(".","/message", request.getPath());
           var mimeType = Files.probeContentType(path);
            out.write((
                    "HTTP/1.1 " + "\r\n" +
                            "Content-Type: " + mimeType +  "\r\n" +
                            "Content-Length: " + Files.size(path) + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write("This is index.html".getBytes());
        });


        // Start
        server.start();
    }
}