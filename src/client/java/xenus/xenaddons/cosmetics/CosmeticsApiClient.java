package xenus.xenaddons.cosmetics;

import xenus.xenaddons.XenAddons;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class CosmeticsApiClient {
    public static final String DEFAULT_PUBLIC_COSMETICS_URL = "http://127.0.0.1:3000/public/cosmetics";
    public static final String API_URL_PROPERTY = "xenaddons.cosmetics.api.url";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final URI endpoint;

    public CosmeticsApiClient() {
        this(
            HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
            resolveConfiguredEndpoint()
        );
    }

    CosmeticsApiClient(HttpClient httpClient, URI endpoint) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint.toString();
    }

    public CosmeticsSnapshot fetchSnapshot() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + endpoint);
        }

        return CosmeticsSnapshotParser.parseString(response.body());
    }

    private static URI resolveConfiguredEndpoint() {
        String configured = System.getProperty(API_URL_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return URI.create(DEFAULT_PUBLIC_COSMETICS_URL);
        }

        try {
            return URI.create(configured);
        } catch (Exception e) {
            XenAddons.LOGGER.warn(
                "Invalid cosmetics API URL in system property {}: {}. Falling back to {}",
                API_URL_PROPERTY,
                configured,
                DEFAULT_PUBLIC_COSMETICS_URL
            );
            return URI.create(DEFAULT_PUBLIC_COSMETICS_URL);
        }
    }
}
