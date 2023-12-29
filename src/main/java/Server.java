import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9999;
    private static final List<String> VALIDPATHS = List.of("/index.html", "/spring.png", "/spring.svg");
    private static final int THREAD_POOL = 64;
    private final ExecutorService executorService;

    public Server() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    executorService.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleConnection(Socket socket) {
        try (socket;
             final var in =new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];
            if (!VALIDPATHS.contains(path)) {
                sendNotResponse(out);
                return;
            }
            final var filePath = Path.of(".","/", path);
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);

            sendResponse(out, mimeType, length);
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendNotResponse(BufferedOutputStream out) throws IOException {
        String response = "HTTP/1.1 404 NOT FOUND \r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close \r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
    private void sendResponse(BufferedOutputStream out, String mimeType, long length) throws IOException {
        String response = "HTTP/1.1 200 OK \r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
    }
}
