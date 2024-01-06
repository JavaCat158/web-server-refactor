import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

    private final String method;
    private final String path;


    public Request(String requestMethod, String requestPath) {
        this.method = requestMethod;
        this.path = requestPath;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
    public String getQueryParam(String name) {
        List<NameValuePair> params = URLEncodedUtils.parse(this.getPath(), StandardCharsets.UTF_8);
        for(NameValuePair param: params) {
            if (param.getName().equals(name)) {
                return param.getValue();
            }
        }
        return null;
    }

    public Map<String, String> getQueryParams() {
        List<NameValuePair> params = URLEncodedUtils.parse(this.getPath(), StandardCharsets.UTF_8);
        Map<String, String> queryParams = new HashMap<>();
        for(NameValuePair param : params) {
            queryParams.put(param.getName(), param.getValue());
        }
        return queryParams;
    }
}