package org.springframework.content.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

public class SolrTestContainer extends SolrContainer {

    private static final String CONNECTION_URL = "http://%s:%d/solr/solr";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("solr:9.8.1");

    private SolrTestContainer() {
        super(IMAGE_NAME);
        // The 'extraction' module provides the Solr Cell ExtractingRequestHandler (/update/extract).
        withEnv("SOLR_MODULES", "extraction");
        start();

        try {
            execInContainer("sh", "-c", "solr create_collection -c solr -d /opt/solr/server/solr/configsets/_default/");

            // The stock _default configset does not expose /update/extract and its _text_
            // catch-all is neither populated (copyField is commented out) nor stored.
            // Register the Solr Cell handler and map the extracted content straight into
            // _text_, then make _text_ stored (+termVectors) so search and highlighting work.
            post("http://localhost:8983/solr/solr/config",
                    "{\"add-requesthandler\":{\"name\":\"/update/extract\","
                            + "\"class\":\"solr.extraction.ExtractingRequestHandler\","
                            + "\"defaults\":{\"lowernames\":true,\"fmap.content\":\"_text_\"}}}");

            post("http://localhost:8983/solr/solr/schema",
                    "{\"replace-field\":{\"name\":\"_text_\",\"type\":\"text_general\","
                            + "\"multiValued\":true,\"indexed\":true,\"stored\":true,"
                            + "\"storeOffsetsWithPositions\":true,\"termVectors\":true,"
                            + "\"termPositions\":true,\"termOffsets\":true}}");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to setup solr container", e);
        }
    }

    private void post(String url, String json) throws IOException, InterruptedException {
        var result = execInContainer("curl", "-sS", "-X", "POST", url,
                "-H", "Content-type:application/json", "-d", json);
        if (result.getExitCode() != 0
                || result.getStdout().contains("errorMessages")
                || result.getStdout().contains("\"error\"")) {
            throw new RuntimeException("Solr admin call failed (" + url + "): "
                    + result.getStdout() + result.getStderr());
        }
    }

    public static SolrClient getSolrClient() {
        String solrUrl = String.format(
                CONNECTION_URL,
                Singleton.INSTANCE.getContainerIpAddress(),
                Singleton.INSTANCE.getMappedPort(SolrContainer.SOLR_PORT));

        // Force HTTP/1.1: the JDK HttpClient's cleartext h2c upgrade against Solr's Jetty
        // is flaky and intermittently fails with "RST_STREAM: Stream cancelled" (notably on CI).
        return new HttpJdkSolrClient.Builder(solrUrl).useHttp1_1(true).build();
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected SolrTestContainer readResolve() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final SolrTestContainer INSTANCE = new SolrTestContainer();
    }
}
