package uk.nhs.adaptors.gpc.consumer.testcontainers;

import org.springframework.util.StringUtils;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.utils.UrlHelpers;

@Slf4j
public final class GpccMocksContainer extends GenericContainer<GpccMocksContainer> {

    public static final int CONTAINER_PORT = 8080;
    public static final String GPC_CONSUMER_SDS_URL = "GPC_CONSUMER_SDS_URL";
    private static GpccMocksContainer container;
    @Getter
    private String mockBaseUrl;

    private GpccMocksContainer() {
        super("docker-gpcc-mocks");
        withExposedPorts(CONTAINER_PORT);
    }

    public static GpccMocksContainer getInstance() {
        LOGGER.warn("!!! The container used for this test is docker_gpcc-mocks built with docker-compose !!!");
        LOGGER.warn("!!! Are your tests failing? Do the mocks seem out of date?                          !!!");
        LOGGER.warn("!!! Run docker-compose build gpcc-mocks to refresh the image                        !!!");
        if (container == null) {
            container = new GpccMocksContainer();
        }
        return container;
    }

    @Override
    public void start() {
        var sdsUrl = System.getenv(GPC_CONSUMER_SDS_URL);
        LOGGER.info("Tests are run with {}={}", GPC_CONSUMER_SDS_URL, sdsUrl);

        if (StringUtils.hasText(sdsUrl)) {
            LOGGER.warn("The gpcc-mocks test container will not run because the '{}' is set", GPC_CONSUMER_SDS_URL);
            this.mockBaseUrl = UrlHelpers.getUrlBase(sdsUrl);
            return;
        }

        try {
            super.start();
        } catch (ContainerLaunchException e) {
            throw new RuntimeException("Have you built the gpcc-mocks container using "
                + "docker-compose yet? See the README for more info.", e);
        }
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
        followOutput(logConsumer);
        var actualHost = getContainerIpAddress();
        var actualPort = getMappedPort(CONTAINER_PORT);
        this.mockBaseUrl = "http://" + actualHost + ":" + actualPort;
        LOGGER.info("Using base url {} for mock container", mockBaseUrl);
        System.setProperty(GPC_CONSUMER_SDS_URL, mockBaseUrl + "/spine-directory/");
        System.setProperty("GPC_CONSUMER_SDS_APIKEY", "anykey");
        System.setProperty("GPC_ENABLE_SDS", "true");
    }
}