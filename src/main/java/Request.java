import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class Request {

    private final String method;
    private final String path;
    private final List<NameValuePair> queryParams;


    public Request(String requestMethod, String requestPath) {
        this.method = requestMethod;
        this.path = requestPath;
        this.queryParams = getQueryParam(requestPath);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    private List<NameValuePair> getQueryParam(String name) {
        return URLEncodedUtils.parse(name, StandardCharsets.UTF_8);
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
}