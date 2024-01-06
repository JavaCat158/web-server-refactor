import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int SERVER_SOCKET;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/classic.html", "/form.html");
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, Map<String, Handler>> handlers;


    public Server(int serverSocket, int poolSize) {
        SERVER_SOCKET = serverSocket;
        executorService = Executors.newFixedThreadPool(poolSize);
        handlers = new ConcurrentHashMap<>();
    }

    void start() {
        try (final var serverSocket = new ServerSocket(SERVER_SOCKET)) {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                executorService.execute(() -> proceedConnection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    private void proceedConnection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            // read only request line for simplicity
            // must be in form GET /path?param1=value1&param2=value2 HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                socket.close();
                return;
            }

            String method = parts[0];
            final var pathWithQuery = parts[1];
            Request request = createRequest(method, pathWithQuery);

            // Check for bad requests and drop connection
            if (request == null || !handlers.containsKey(request.getMethod())) {
                String responseContent = "Not Found";
                responseWithContent(out, "400", "Bad Request", responseContent.getBytes());
                return;
            }

            // Get PATH, HANDLER Map
            Map<String, Handler> handlerMap = handlers.get(request.getMethod());
            String requestPath = request.getPath().split("\\?")[0]; // игнорируем QueryString
            if (handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
            } else {  // Defaults
                // Resource not found
                if (!validPaths.contains(request.getPath())) {
                    responseWithContent(out, "404", "Not Found", "NO!!!".getBytes());
                } else {
                    defaultHandler(out, requestPath);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void defaultHandler(BufferedOutputStream out, String path) throws IOException {
        final var filePath = Path.of(".", "message", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
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
            return;
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private Request createRequest(String method, String path) {
        // TODO: More checks fo r bad fields
        if (method != null && !method.isBlank()) {
            return new Request(method, path);
        } else {
            return null;
        }

    }

    void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    void responseWithContent(BufferedOutputStream out, String responseCode, String responseStatus, byte[] content) throws IOException {
        out.write((
                "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }
}