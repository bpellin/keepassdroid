package net.temerity.davsync;

import java.net.URI;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;


public class HttpPropFind extends HttpEntityEnclosingRequestBase implements HttpEntityEnclosingRequest {

	private final int DEPTH = 1;
    public final static String METHOD_NAME = "PROPFIND";

    public HttpPropFind() {
        super();
        this.setHeader("Content-Type", "text/xml");
        this.setDepth(DEPTH);
    }

    public HttpPropFind(final URI uri) {
        super();
        this.setHeader("Content-Type", "text/xml");
        this.setDepth(DEPTH);
        this.setURI(uri);
    }

    public void setDepth(int val) {
        this.setHeader("Depth", String.valueOf(val));
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpPropFind(final String uri) {
        super();
        this.setHeader("Content-Type", "text/xml");
        this.setDepth(DEPTH);
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
    
}
