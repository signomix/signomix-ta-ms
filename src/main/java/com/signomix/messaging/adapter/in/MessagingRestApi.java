package com.signomix.messaging.adapter.in;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.signomix.annotation.InboundAdapter;
import com.signomix.messaging.adapter.out.SmtpAdapter;
import com.signomix.messaging.application.port.in.MailingPort;
import com.signomix.messaging.dto.MessageWrapper;

import io.quarkus.logging.Log;

@InboundAdapter
@ApplicationScoped
@Path("/api/ms")
public class MessagingRestApi {

    @Inject
    SmtpAdapter mailerService;

    @Inject
    MailingPort mailingPort;

    @PostConstruct
    void init() {
        Log.infof("Starting");
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHealth() {
        return "OK";
    }

    @POST
    @Path("/webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String sendToWebhook(@HeaderParam("Authorization") String token, MessageWrapper message) {
        Log.info(message.eui + " " + message.message);
        return "OK";
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response send(
            @HeaderParam("Authorization") String token,
            MultivaluedMap<String, String> form) {

        String documentUid = form.getFirst("doc");
        String target = form.getFirst("target");
        mailingPort.sendDocument(documentUid, target, token);
        return Response.ok().build();
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
