package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CallbackClientTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private HttpClient httpClient;
    private CallbackClient callbackClient;

    @Before
    public void init() {
        callbackClient = new CallbackClient(httpClient);
    }

    @Test
    public void givenCallbackListenerDoesNotRespondWhenSendingARequestThenResponseIsNotOk() throws IOException, InterruptedException {
        givenCallbackListenerDoesNotRespond();
        CallbackClient.Response response = callbackClient.callback(new URL("http://callback.example.com"));
        assertFalse(response.ok());
        assertFalse(response.notFound());
    }

    private void givenCallbackListenerDoesNotRespond() throws IOException, InterruptedException {
        givenCallbackListenerRespondsWith();
    }

    @SuppressWarnings("unchecked")
    private void givenCallbackListenerRespondsWith() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(null);
    }


}
