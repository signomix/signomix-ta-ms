package com.signomix.messaging.adapter.in;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.annotation.InboundAdapter;
import com.signomix.common.news.NewsDefinition;
import com.signomix.messaging.adapter.out.MailingActionRepository;
import com.signomix.messaging.application.port.in.MailingPort;
import com.signomix.messaging.domain.AuthLogic;
import com.signomix.messaging.domain.news.NewsLogic;

import io.quarkus.logging.Log;

@InboundAdapter
@ApplicationScoped
@Path("/api/ms")
public class MessagingRestApi {

    @Inject
    Logger logger = Logger.getLogger(MessagingRestApi.class);

    @Inject
    MailingActionRepository repository;

    // @Inject
    // SmtpAdapter mailerService;

    @Inject
    MailingPort mailingPort;

    @Inject
    NewsLogic newsLogic;

    @PostConstruct
    void init() {
        Log.info("Starting");
    }

    @Inject
    AuthLogic authLogic;

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHealth() {
        repository.listAll().forEach(action -> {
            System.out.println(action.getStatus());
        });
        return "OK";
    }

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String sendToWebhook(@HeaderParam("Authorization") String token, MessageEnvelope message) {
        Log.info(message.eui + " " + message.message);
        return "OK";
    }

    @Transactional
    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(
            @HeaderParam("Authorization") String token,
            MultivaluedMap<String, String> form) {

        // TODO: check token
        String documentPath = form.getFirst("doc");
        String target = form.getFirst("target");

        // mailingPort.sendDocument(documentUid, target, token);
        mailingPort.addPlannedMailing(documentPath, target, token);
        return Response.ok().build();
    }

    /**
     * Send news to selected users
     * 
     * @param token      API token
     * @param definition News definition
     */
    @POST
    @Path("/news")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendNews(@HeaderParam("Authentication") String token, NewsDefinition definition) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("user not found").build();
        }
        newsLogic.sendNews(user, definition);
        return Response.ok().build();
    }

    @GET
    @Path("/news")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNews(@HeaderParam("Authentication") String token,
            @QueryParam("language") String language,
            @QueryParam("limit") Long limit,
            @QueryParam("offset") Long offset) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("user not found").build();
        }
        return Response.ok().entity(newsLogic.getNewsForUser(user, language, limit!=null?limit:20, offset!=null?offset:0)).build();
    }

    /**
     * Get news issue
     */
    @GET
    @Path("/news/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNewsIssue(@HeaderParam("Authentication") String token, @PathParam("id") Long id,
            @QueryParam("language") String language) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("user not found").build();
        }
        return Response.ok().entity(newsLogic.getNewsIssue(user,id,language)).build();
    }

    /*
     * 
     * @POST
     * 
     * @Path("/email")
     * 
     * @Consumes(MediaType.APPLICATION_JSON)
     * 
     * @Produces(MediaType.TEXT_PLAIN)
     * public String sendASimpleEmail(MessageWrapper messageWrapper) {
     * return mailerService.send(messageWrapper.user.email, messageWrapper.subject,
     * messageWrapper.message);
     * }
     * 
     * @POST
     * 
     * @Path("/send")
     * 
     * @Consumes(MediaType.APPLICATION_JSON)
     * 
     * @Produces(MediaType.TEXT_PLAIN)
     * public String sendMessage(MessageWrapper messageWrapper) {
     * Log.infof("MESSAGE: %1$s %2$s", messageWrapper.eui, messageWrapper.message);
     * ChannelConfig channelConfig = new
     * ChannelConfig(getChannelConfig(messageWrapper.type, messageWrapper.user));
     * if (channelConfig.address == null){
     * return null; // OK its normal behaviour
     * }
     * String response = "";
     * switch (channelConfig.channelType.toUpperCase()) {
     * case "SMTP":
     * response = mailerService.send(channelConfig.address, messageWrapper.eui,
     * messageWrapper.message);
     * break;
     * case "SMS":
     * if (messageWrapper.user.credits > 0) {
     * response = "";
     * }
     * if (!response.startsWith("ERROR")) {
     * //TODO: decrease user credits
     * }
     * break;
     * case "PUSHOVER":
     * //response = pushoverService.send(address, nodeName, messageContent);
     * break;
     * case "SLACK":
     * //response = slackService.send(address, nodeName, messageContent);
     * break;
     * case "TELEGRAM":
     * //response = telegramService.send(address, nodeName, messageContent);
     * break;
     * case "DISCORD":
     * new DiscordService().send(channelConfig.address, new
     * DiscordMessage("y","tets2"));
     * response = "OK";
     * break;
     * case "WEBHOOK":
     * new WebhookService().send(channelConfig.address, new Message("y","tets2"));
     * response ="OK";
     * break;
     * default:
     * Log.warnf("message channel %1$s not supported", channelConfig.channelType);
     * }
     * if (response.startsWith("ERROR")) {
     * Log.warn(response);
     * }
     * return "";
     * }
     * 
     * @POST
     * 
     * @Path("/send/{id}")
     * 
     * @Produces(MediaType.TEXT_PLAIN)
     * 
     * @Consumes(MediaType.APPLICATION_JSON)
     * public String sendMessage(@PathParam String id, MessageWrapper
     * messageWrapper) {
     * System.out.println("ID: " + id);
     * System.out.println("MESSAGE: " + messageWrapper.eui + " " +
     * messageWrapper.message);
     * return "";
     * }
     * 
     * @POST
     * 
     * @Path("/send/{id}/{eui}")
     * 
     * @Produces(MediaType.TEXT_PLAIN)
     * public String sendMessage(@PathParam String id, @PathParam String eui) {
     * System.out.println("ID: " + id);
     * System.out.println("EUI: " + eui);
     * return "";
     * }
     * 
     * 
     * 
     * private String[] getChannelConfig(String eventTypeName, User user) {
     * String channel;
     * switch (eventTypeName.toUpperCase()) {
     * case "GENERAL":
     * case "DEVICE_LOST":
     * channel = user.generalNotificationChannel;
     * break;
     * case "INFO":
     * channel = user.infoNotificationChannel;
     * break;
     * case "WARNING":
     * channel = user.warningNotificationChannel;
     * break;
     * case "ALERT":
     * channel = user.alertNotificationChannel;
     * break;
     * default:
     * channel = "";
     * }
     * return channel.split(":");
     * }
     * 
     */

}
