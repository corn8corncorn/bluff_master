package com.bluffmaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

  @Bean
  public RestTemplate restTemplate(
      @Value("${game.fake-image.fetch-timeout-ms:20000}") int timeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    RestTemplate restTemplate = new RestTemplate(factory);
    restTemplate.setRequestFactory(factory);
    restTemplate
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request
                  .getHeaders()
                  .set(
                      "User-Agent",
                      "BluffMaster/1.0 (party-game; +https://github.com/bluff-master)");
              return execution.execute(request, body);
            });
    return restTemplate;
  }
}
