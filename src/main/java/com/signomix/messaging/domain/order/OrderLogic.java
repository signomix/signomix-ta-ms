package com.signomix.messaging.domain.order;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.billing.Order;
import com.signomix.common.db.BillingDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.tsdb.BillingDao;
import com.signomix.messaging.adapter.out.HcmsService;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;
import com.signomix.messaging.domain.Document;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class OrderLogic {

    @Inject
    Logger logger;

    /*
     * @Inject
     * MailerService mailerService;
     */

    @Inject
    MessageProcessorAdapter messagePort;

    @ConfigProperty(name = "signomix.mqtt.field.separator", defaultValue = ";")
    String mqttFieldSeparator;

    @ConfigProperty(name = "signomix.api.url", defaultValue = "https://localhost")
    String apiUrl;
    @ConfigProperty(name = "signomix.webapp.url", defaultValue = "https://cloud.localhost")
    String webappUrl;

    @ConfigProperty(name = "signomix.hcms.api.path", defaultValue = "")
    String hcmsApiPath;
    /**
     * Path to the document with the ask-to-confirm message. Resulting path using
     * defaultValue given below will be: 
     * hcmsApiPath + /templates/{language}/ +
     * askToConfirmDocumentPath
     */
    @ConfigProperty(name = "signomix.hcms.order-template.html", defaultValue = "order-template.html")
    String orderTemplateDocumentPath;

    @ConfigProperty(name = "signomix.admin.email", defaultValue = "")
    String adminEmail;

    @Inject
    @DataSource("billing")
    AgroalDataSource billingDataSource;

    BillingDaoIface billingDao;

    @Inject
    HcmsService hcmsService;

    void onStart(@Observes StartupEvent ev) {
        billingDao = new BillingDao();
        billingDao.setDatasource(billingDataSource);
    }

    public void processOrderEvent(String message) {
        logger.info("Order event: " + message);
        String[] parts = message.split(mqttFieldSeparator);
        if (parts.length < 2) {
            logger.error("Wrong user event message: " + message);
            return;
        }
        String eventType = parts[0];
        String orderId = parts[1];

        switch (eventType.toLowerCase()) {
            case "created":
                sendAdminEmail(orderId);
                break;
            default:
        }
    }

    private void sendAdminEmail(String orderId) {
        Order order = null;
        try {
            order = billingDao.getOrder(orderId);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == order) {
            logger.error("Order not found: " + orderId);
            return;
        }

        MessageEnvelope envelope = new MessageEnvelope();
        User user = new User();
        user.email = adminEmail;
        envelope.message = "Order created: " + orderId;
        envelope.subject = "Order created: " + orderId;
        envelope.user = user;
        messagePort.processAdminEmail(envelope);
    }

    /**
     * Sends an email with a confirmation link to the user.
     * @param userLogin
     */
    /* private void sendAdminEmail(String orderId) {
        Order order = null;
        try {
            order = billingDao.getOrder(orderId);
        } catch (IotDatabaseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        if (null == order) {
            logger.error("Order not found: " + orderId);
            return;
        }

        Document doc = null;
        //if (user.preferredLanguage.equalsIgnoreCase("pl")) {
            doc = hcmsService.getDocument(hcmsApiPath + "/pl/" + orderTemplateDocumentPath);
        //} else {
        //    doc = hcmsService.getDocument(hcmsApiPath + "/en/" + askToConfirmDocumentPath);
        //}
        if (null == doc) {
            logger.info("Document not found: " + orderTemplateDocumentPath);
            return;
        }
        String message = doc.content;
        HashMap<String,String> valueMap = new HashMap<>();
        valueMap.put("API_URL", apiUrl);
        valueMap.put("WEBAPP_URL", webappUrl);
        valueMap.put("ORDER_NO", order.id);
        valueMap.put("DATE", order.createdAt.toString());
        valueMap.put("NAME", order.name);
        valueMap.put("SURNAME", order.surname);
        valueMap.put("EMAIL", order.email);
        valueMap.put("ADDRESS", order.address);
        valueMap.put("CITY", order.city);
        valueMap.put("ZIP", order.zip);
        valueMap.put("COUNTRY", order.country);
        valueMap.put("COMPANY", order.companyName);
        valueMap.put("TAX_NO", order.vat);
        valueMap.put("PROVIDER", "EXPERIOT Grzegorz Skorupa");
        valueMap.put("PROVIDER_TAX_NO", "PL8271132580");
        valueMap.put("PROVIDER_WWW", "https://experiot.pl");
        valueMap.put("SERVICE_NAME", "Signomix");
        valueMap.put("SERVICE_PRICE", order.price);
        valueMap.put("SERVICE_VAT", order.targetType);
        valueMap.put("SERVICE_TOTAL", order.totalPrice);

        message = replacePlaceholders(message, valueMap);
        String subject = doc.metadata.get("subject");

        MessageEnvelope envelope = new MessageEnvelope();
        User user = new User();
        user.email = adminEmail;
        envelope.message = message;
        envelope.subject = subject;
        envelope.user = user;
        messagePort.processAdminEmail(envelope);
    } */

    

    /**
     * Finds all words starting with { and ending with }.
     * 
     * @param text
     * @return list of words
     */
    public static String[] findPlaceholders(String text) {
        return text.split("\\{[^\\}]*\\}");
    }

    /**
     * Replaces placeholders in the text with values and User fields.
     * 
     * @param text
     * @param user
     * @param valueMap
     * @return
     */
    public static String replacePlaceholders(String text, HashMap<String,String> valueMap) {
        String result = text;
        String[] placeholders = findPlaceholders(text);
/*         result = result.replace("{USER_NAME}", user.name);
        result = result.replace("{USER_SURNAME}", user.surname);
        result = result.replace("{USER_EMAIL}", user.email);
        result = result.replace("{USER_SECRET}", user.confirmString); */
        for(int i=0;i<placeholders.length;i++) {
            String key = placeholders[i].substring(1,placeholders[i].length()-1);
            if(valueMap.containsKey(key)) {
                result = result.replace("{"+key+"}", valueMap.get(key));
            }
        }
        return result;
    }

}
