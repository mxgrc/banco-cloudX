package cl.bancocloud.bff.integration;

import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientsConfig {
    @Bean(destroyMethod="close")
    CloseableHttpClient downstreamHttpClient() {
        var manager=PoolingHttpClientConnectionManagerBuilder.create().setDefaultConnectionConfig(
                ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(2)).build()).build();
        return HttpClients.custom().setConnectionManager(manager).disableRedirectHandling().disableAutomaticRetries().build();
    }
    @Bean
    HttpComponentsClientHttpRequestFactory downstreamFactory(CloseableHttpClient client) {
        var factory=new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectionRequestTimeout(2000);
        factory.setReadTimeout(10000);
        return factory;
    }
    @Bean("usuariosClient")
    RestClient usuarios(RestClient.Builder builder, HttpComponentsClientHttpRequestFactory factory,
            @Value("${banco.usuarios-url}") String url) { return client(builder,factory,url); }
    @Bean("beneficiosClient")
    RestClient beneficios(RestClient.Builder builder, HttpComponentsClientHttpRequestFactory factory,
            @Value("${banco.beneficios-url}") String url) { return client(builder,factory,url); }
    @Bean("inscripcionesClient")
    RestClient inscripciones(RestClient.Builder builder, HttpComponentsClientHttpRequestFactory factory,
            @Value("${banco.inscripciones-url}") String url) { return client(builder,factory,url); }
    private RestClient client(RestClient.Builder builder,HttpComponentsClientHttpRequestFactory factory,String url) {
        return builder.clone().baseUrl(url).requestFactory(factory).build();
    }
}
