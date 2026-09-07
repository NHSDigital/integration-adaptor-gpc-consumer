package uk.nhs.adaptors.gpc.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.adaptors.gpc.consumer.filters.LoggingGlobalFilter;
import uk.nhs.adaptors.gpc.consumer.filters.SdsFilter;
import uk.nhs.adaptors.gpc.consumer.filters.SspFilter;
import uk.nhs.adaptors.gpc.consumer.filters.TlsMutualAuthRoutingFilter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ConsumerApplication.class, webEnvironment = RANDOM_PORT)

public class PipelineFilterOrderTest {

    @Autowired
    private ApplicationContext applicationContext;

    private static final String DOMAIN_PREFIX = "uk.nhs.adaptors";

    @Test
    void When_StartingApplication_Expect_FiltersShouldBeLoadedInCorrectOrder() {

        var filters = applicationContext
                .getBeansOfType(GlobalFilter.class)
                .values()
                .stream()
                .filter(globalFilter -> globalFilter.getClass().getName().startsWith(DOMAIN_PREFIX))
                .sorted(Comparator.comparingInt(f -> ((Ordered) f).getOrder()))
                .toList();

        assertThat(filters)
                .withFailMessage("Filters are not loaded in the correct order.")
                .hasExactlyElementsOfTypes(
                        SdsFilter.class,
                        SspFilter.class,
                        LoggingGlobalFilter.class,
                        TlsMutualAuthRoutingFilter.class);
    }
}
