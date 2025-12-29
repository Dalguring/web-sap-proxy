package com.prototype.proxy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("dev");

        return new OpenAPI()
            .info(new Info()
                .title("Interface Proxy API")
                .version("v1")
                .description("SAP RFC 호출을 위한 인터페이스 프록시 서버 API 문서"))
            .servers(List.of(devServer));
    }
}
