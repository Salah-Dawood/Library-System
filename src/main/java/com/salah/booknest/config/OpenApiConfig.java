package com.salah.booknest.config;

import com.salah.booknest.model.response.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";
    private static final String API_ERROR_REF = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI bookNestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookNest API")
                        .version("1.0")
                        .description("""
                                Library system: members request and return books, librarians approve loans and manage \
                                the catalogue and users.

                                **How to try it**
                                1. `POST /auth/users/register`, then `PUT /auth/email/getverification/{username}` and \
                                `PUT /auth/email/verify/{username}?code=...`
                                2. `POST /auth/users/login` and copy the `token` from the response
                                3. Click **Authorize**, paste the token (without the word Bearer), then call any endpoint

                                Endpoints marked with a lock need a token. Errors always use the same JSON shape \
                                (`timestamp`, `status`, `error`, `message`, `path`, and `fieldErrors` for validation)."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT from POST /auth/users/login")))
                // Secured by default; the public /auth endpoints are switched off in the customizer below.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /** Adds the shared error schema and the error responses that apply to every operation. */
    @Bean
    public OpenApiCustomizer errorResponses() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ApiError.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                boolean isPublic = path.startsWith("/auth/");
                if (isPublic) {
                    operation.setSecurity(List.of());
                }

                ApiResponses responses = operation.getResponses();
                // Endpoints that answer 201 or 204 have no plain 200.
                if (responses.containsKey("201") || responses.containsKey("204")) {
                    responses.remove("200");
                }
                if (!isPublic) {
                    responses.putIfAbsent("403", new ApiResponse()
                            .description("Forbidden: missing or invalid token, or your role is not allowed"));
                }
                if (method != PathItem.HttpMethod.GET && method != PathItem.HttpMethod.DELETE) {
                    responses.putIfAbsent("400", new ApiResponse()
                            .description("Validation failed (see fieldErrors) or the body is malformed"));
                }
                responses.putIfAbsent("500", new ApiResponse().description("Unexpected server error"));

                responses.forEach((code, response) -> {
                    if ((code.startsWith("4") || code.startsWith("5")) && response.getContent() == null) {
                        response.setContent(errorBody());
                    }
                });
            }));
        };
    }

    private static Content errorBody() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref(API_ERROR_REF)));
    }
}
