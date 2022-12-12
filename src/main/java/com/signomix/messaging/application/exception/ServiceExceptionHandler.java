package com.signomix.messaging.application.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ServiceExceptionHandler implements ExceptionMapper<ServiceException> {

    @ConfigProperty(name = "com.signomix.messaging.exception.user.not.found")
    String userNotFound;
    @ConfigProperty(name = "com.signomix.messaging.exception.document.not.found")
    String documentNotFound;
    @ConfigProperty(name = "com.signomix.messaging.exception.unauthorized")
    String unauthorized;

    @Override
    public Response toResponse(ServiceException e) {

        if (e.getMessage().equalsIgnoreCase(userNotFound)) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorMessage(e.getMessage()))
                    .build();
        }else if (e.getMessage().equalsIgnoreCase(documentNotFound)) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ErrorMessage(e.getMessage()))
                    .build();
        }else if (e.getMessage().equalsIgnoreCase(unauthorized)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorMessage(e.getMessage()))
                    .build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage()))
                    .build();
        }
    }
}
