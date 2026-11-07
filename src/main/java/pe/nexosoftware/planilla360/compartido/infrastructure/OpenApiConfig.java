package pe.nexosoftware.planilla360.compartido.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String ESQUEMA = "bearer-jwt";

    @Bean
    OpenAPI planilla360OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Planilla360 API")
                        .version("v1")
                        .description("API de RRHH y planillas para pymes peruanas (Nexo Software S.A.C.)."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
