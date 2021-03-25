package uk.nhs.adaptors.gpc.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import uk.nhs.adaptors.gpc.consumer.utils.QueryParamsEncoder;

@ExtendWith(MockitoExtension.class)
public class QueryParamsEncoderTest {
    private static final String FIRST_PARAM = "first";
    private static final String SECOND_PARAM = "second";
    private static final String TEST_URI = "http://www.testhost.com";
    private static final String TEST_URI_WITH_REGULAR_PARAMETERS
        = "http://www.testhost.com?first=firstValue&second=secondValue";
    private static final String TEST_URI_WITH_ENCODING_PARAMETERS
        = "http://www.testhost.com?first=test1:test2&second=tes1|test2";
    private static final String TEST_URI_WITH_ENCODED_PARAMETERS
        = "http://www.testhost.com?first=test1%3Atest2&second=test1%7Ctest2";
    private static final Map<String, Object> ATTRIBUTES = new HashMap<>();

    private static MultiValueMap<String, String> regularQueryParams;
    private static MultiValueMap<String, String> encodingQueryParams;

    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest serverHttpRequest;

    @BeforeAll
    public static void initialize() {
        regularQueryParams = new LinkedMultiValueMap<>();
        regularQueryParams.add(FIRST_PARAM, "firstValue");
        regularQueryParams.add(SECOND_PARAM, "secondValue");

        encodingQueryParams = new LinkedMultiValueMap<>();
        encodingQueryParams.add(FIRST_PARAM, "test1:test2");
        encodingQueryParams.add(SECOND_PARAM, "test1|test2");
    }

    @BeforeEach
    public void setUp() {
        when(exchange.getAttributes()).thenReturn(ATTRIBUTES);
        when(exchange.getRequest()).thenReturn(serverHttpRequest);
    }

    @AfterEach
    public void tearDown() {
        ATTRIBUTES.clear();
    }

    @ParameterizedTest
    @MethodSource("uriParams")
    public void When_EncodingUri_Expect_UriWithProperlyEncodedParams(String inputUri,
            MultiValueMap<String, String> queryParams,
            String outputUri) {
        when(serverHttpRequest.getQueryParams()).thenReturn(queryParams);

        ATTRIBUTES.put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, prepareUri(inputUri, queryParams));
        QueryParamsEncoder.encodeQueryParams(exchange);
        URI uri =  (URI) exchange.getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        assertThat(uri.toString()).isEqualTo(outputUri);
    }

    private static Stream<Arguments> uriParams() {
        return Stream.of(
            Arguments.of(TEST_URI, new LinkedMultiValueMap<>(), TEST_URI),
            Arguments.of(TEST_URI_WITH_REGULAR_PARAMETERS, regularQueryParams, TEST_URI_WITH_REGULAR_PARAMETERS),
            Arguments.of(TEST_URI_WITH_ENCODING_PARAMETERS, encodingQueryParams, TEST_URI_WITH_ENCODED_PARAMETERS)
        );
    }

    private URI prepareUri(String uri, MultiValueMap<String, String> queryParams) {
        return UriComponentsBuilder.fromUriString(uri)
            .queryParams(queryParams)
            .build()
            .toUri();
    }
}
