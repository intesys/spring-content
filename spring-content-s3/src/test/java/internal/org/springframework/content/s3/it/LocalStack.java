package internal.org.springframework.content.s3.it;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.net.URIBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class LocalStack extends LocalStackContainer implements Serializable {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("localstack/localstack:4.9.0");

    private LocalStack() {
        super(IMAGE_NAME);
        withServices(Service.S3);
        start();
    }

    private static class Singleton {
        private static final LocalStack INSTANCE = new LocalStack();
    }

    public static S3Client getAmazonS3Client() throws URISyntaxException {
        return S3Client.builder()
                .endpointOverride(Singleton.INSTANCE.getEndpointOverride(Service.S3))
                .region(Region.US_EAST_1) // Set a region, so it does not need to be configured externally
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        Singleton.INSTANCE.getAccessKey(), Singleton.INSTANCE.getSecretKey())))
                .serviceConfiguration((bldr) -> bldr.pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public URI getEndpointOverride(EnabledService service) {
        try {
            // super method converts localhost to 127.0.0.1 which fails on macos
            // need to revert it back to whatever getContainerIpAddress() returns
            return new URIBuilder(super.getEndpointOverride(service)).setHost(getContainerIpAddress()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    @SuppressWarnings("unused") // Serializable safe singleton usage
    protected LocalStack readResolve() {
        return Singleton.INSTANCE;
    }
}
