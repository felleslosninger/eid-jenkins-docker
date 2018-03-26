package no.difi.pipeline.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

public class CallbackClientTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private RestTemplate httpClient;
    private CallbackClient callbackClient;

    @Before
    public void init() {
        callbackClient = new CallbackClient(httpClient);
    }

    @Test
    public void givenCallbackListenerDoesNotRespondWhenSendingARequestThenResponseIsNotOk() throws IOException {
        givenCallbackListenerDoesNotRespond();
        CallbackClient.Response response = callbackClient.callback(new URL("http://callback.example.com"));
        assertFalse(response.ok());
        assertFalse(response.notFound());
    }

    private void givenCallbackListenerDoesNotRespond() {
        givenCallbackListenerRespondsWith();
    }

    @SuppressWarnings("unchecked")
    private void givenCallbackListenerRespondsWith() {
        when(httpClient.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(null);
    }


}
