package com.acme.payments;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import com.acme.security.SignatureAuthenticationFilter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentsApplicationTests {

    private static final String SECRET = "demo-shared-secret";

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String body, String signature) throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/payments"))
                .header("Content-Type", "application/json")
                .header(SignatureAuthenticationFilter.MERCHANT, "shop-42")
                .header(SignatureAuthenticationFilter.TIMESTAMP, ts)
                .header(SignatureAuthenticationFilter.SIGNATURE,
                        signature != null ? signature : SignatureAuthenticationFilter.sign(SECRET, "shop-42", ts))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void smallPaymentIsAuthorized() throws Exception {
        HttpResponse<String> r = post("{\"amount\":100,\"currency\":\"EUR\"}", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("AUTHORIZED").contains("shop-42");
    }

    @Test
    void largePaymentIsDeclinedByFraudScreening() throws Exception {
        HttpResponse<String> r = post("{\"amount\":9000,\"currency\":\"EUR\"}", null);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("DECLINED");
    }

    @Test
    void badSignatureIsRejected() throws Exception {
        HttpResponse<String> r = post("{\"amount\":100,\"currency\":\"EUR\"}", "deadbeef");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.body()).contains("invalid_signature");
    }

    @Test
    void unsignedRequestIsRejected() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/payments"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(401);
    }

    @Test
    void healthIncludesFraudScreeningFromStarter() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("fraudScreening");
    }
}
