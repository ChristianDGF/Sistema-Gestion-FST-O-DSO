package proyecto.sistemaGestion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestión de Inventarios API")
                        .description("API REST para el sistema de gestión de inventarios empresarial " +
                                "con Full Stack Testing, Observabilidad y DevSecOps")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Christian David Gutierrez Filpo")
                                .email("christiandg1308@gmail.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido de Keycloak"))
                        .addSecuritySchemes("oauth2ClientCredentials", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Client Credentials Grant para socios externos (M2M)")
                                .flows(new OAuthFlows()
                                        .clientCredentials(new OAuthFlow()
                                                .tokenUrl("http://localhost:8080/realms/sistema-gestion/protocol/openid-connect/token")
                                                .scopes(new Scopes()
                                                        .addString("external:product:read", "Consultar productos")
                                                        .addString("external:stock:read", "Consultar movimientos de stock"))))));
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("internal-api")
                .displayName("API Interna")
                .pathsToExclude("/api/external/**")
                .build();
    }

    @Bean
    public GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("external-api")
                .displayName("API Empresarial Externa")
                .pathsToMatch("/api/external/**")
                .build();
    }
}
