package hub.ebb.jblcluster.verticles.jpsEvent;

import io.vertx.ext.web.RoutingContext;


//@EndPointVerticleAnnotation
public interface JpsEventVerticleEndPoint {
//    @Operation(summary = "Push a jps Event to Jbl",
//            method = "POST",
//            operationId = "jbl/api/peripherals/:peripheralId/event/:sequenceNumberTS/:sequenceNumberGMT/:sequenceNumberCounter",
//            description = "This service permit to send a JPS event to JBL.</br>" +
//                    "Every event it's typed by a ps",
//            tags = {"JpsEventVerticle"},
//            parameters = {
//                    @Parameter(in = ParameterIn.HEADER,
//                            name = "authToken",
//                            required = true,
//                            description = "Authetication token give by the authentication service for the peripheral that made the validation",
//                            schema = @Schema(implementation = String.class)),
//                    @Parameter(in = ParameterIn.PATH,
//                            name = "peripheralId",
//                            required = true,
//                            description = "The id of peripheral. It has to be unique for each peripheral. Typically it's the MAC ADDRESS",
//                            schema = @Schema(implementation = String.class)
//                    ),
//                    @Parameter(in = ParameterIn.PATH,
//                            name = "sequenceNumberTS",
//                            required = true,
//                            description = "Unix Time stamp of the event happened on the peripheral. It has to be UTC",
//                            schema = @Schema(implementation = String.class)
//                    ),
//                    @Parameter(in = ParameterIn.PATH,
//                            name = "sequenceNumberGMT",
//                            required = true,
//                            description = "GMT of the timestamp of the event",
//                            schema = @Schema(implementation = String.class)
//                    ),
//                    @Parameter(in = ParameterIn.PATH,
//                            name = "sequenceNumberCounter",
//                            required = true,
//                            description = "Counter of the event made on Peripheral",
//                            schema = @Schema(implementation = String.class)
//                    )
//            },
//            requestBody = @RequestBody(
//                    description = "Type of event sent from JPS Peripheral",
//                    content = @Content(
//                            mediaType = "application/json",
//                            encoding = @Encoding(contentType = "application/json"),
//                            schema = @Schema(name = "product", implementation = JpsEvent.class)
//                    ),
//                    required = true
//            ),
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "OK",
//                            content = @Content(
//                                    mediaType = "application/json",
//                                    encoding = @Encoding(contentType = "application/json"),
//                                    schema = @Schema(implementation = String.class)
//                            )
//                    ),
//                    @ApiResponse(responseCode = "404", description = "Not Found"),
//                    @ApiResponse(responseCode = "500", content = @Content(
//                            mediaType = "application/json",
//                            encoding = @Encoding(contentType = "application/json"),
//                            schema = @Schema(implementation = GenericResponse.class)
//                    ),
//                            description = "Internal Server Error.")
//            }
//    )
    void doEventManagement(RoutingContext routingContext);
}
