package com.signomix.messaging.domain.order;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.signomix.common.MessageEnvelope;
import com.signomix.common.User;
import com.signomix.common.billing.Order;
import com.signomix.common.billing.ValueToTextConverter;
import com.signomix.common.db.BillingDaoIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.hcms.Document;
import com.signomix.common.tsdb.BillingDao;
import com.signomix.messaging.adapter.out.HcmsService;
import com.signomix.messaging.adapter.out.MessageProcessorAdapter;

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
    @ConfigProperty(name = "signomix.hcms.order-com-template.html", defaultValue = "order-com-template.html")
    String orderComTemplateDocumentPath;
    @ConfigProperty(name = "signomix.hcms.proforma-template.html", defaultValue = "proforma-template.html")
    String proformaTemplateDocumentPath;
    @ConfigProperty(name = "signomix.hcms.proforma-com-template.html", defaultValue = "proforma-com-template.html")
    String proformaComTemplateDocumentPath;

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
                sendEmails(orderId);
                break;
            default:
        }
    }

    private void sendEmails(String orderId) {
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

        Document orderTemplate = null;
        Document proformaTemplate = null;
        try {
            if (order.taxNumber == null || order.taxNumber.isEmpty()) {
                orderTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + orderTemplateDocumentPath);
            } else {
                //orderTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + orderComTemplateDocumentPath);
                orderTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + orderTemplateDocumentPath);
            }
            if (null == orderTemplate) {
                logger.info("Document not found: " + orderTemplateDocumentPath);
                return;
            }
            if (order.taxNumber == null || order.taxNumber.isEmpty()) {
                proformaTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + proformaTemplateDocumentPath);
            } else {
                //proformaTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + proformaComTemplateDocumentPath);
                proformaTemplate = hcmsService.getDocument(hcmsApiPath + "/pl/" + proformaTemplateDocumentPath);
            }
            if (null == proformaTemplate) {
                logger.info("Document not found: " + proformaTemplateDocumentPath);
                return;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        // get variable values from the document metadata
        // TODO: move variables to the database
        String providerName = orderTemplate.metadata.get("providerName");
        String providerTaxNo = orderTemplate.metadata.get("providerTaxNo");
        String providerHomepage = orderTemplate.metadata.get("providerHomepage");
        String serviceName = orderTemplate.metadata.get("serviceName");
        String providerBank = orderTemplate.metadata.get("providerBank");
        String providerBankAccount = orderTemplate.metadata.get("providerBankAccount");

        Locale locale = new Locale("pl", "PL");
        HashMap<String, String> valueMap = new HashMap<>();
        // valueMap.put("API_URL", apiUrl);
        // valueMap.put("WEBAPP_URL", webappUrl);
        valueMap.put("$ORDER_NO$", order.id);
        valueMap.put("$INVOICE_NO$", order.id);
        valueMap.put("$DATE$", formatTimestampToLocalDate(order.createdAt));
        valueMap.put("$LOGIN$", order.uid);
        valueMap.put("$COMPANY$", order.companyName);
        valueMap.put("$NAME$", order.name);
        valueMap.put("$SURNAME$", order.surname);
        valueMap.put("$EMAIL$", order.email);
        valueMap.put("$ADDRESS$", order.address);
        valueMap.put("$CITY$", order.city);
        valueMap.put("$ZIP$", order.zip);
        valueMap.put("$COUNTRY$", order.country);
        valueMap.put("$TAX_NO$", order.taxNumber);
        valueMap.put("$SERVICE_TAX$", order.tax);
        valueMap.put("$SERVICE_PRICE$", "" + formatCurrency(order.price,locale));
        valueMap.put("$SERVICE_VAT$", "" + formatCurrency(order.vatValue,locale));
        valueMap.put("$SERVICE_VALUE$", "" + formatCurrency(order.total,locale));
        valueMap.put("$SERVICE_VALUE_TEXT$", ValueToTextConverter.getValueAsText(order.total));
        valueMap.put("$PROVIDER_NAME$", providerName);
        valueMap.put("$PROVIDER_TAX_NO$", providerTaxNo);
        valueMap.put("$PROVIDER_WWW$", providerHomepage);
        valueMap.put("$SERVICE_NAME$", order.serviceName);
        valueMap.put("$BANK_NAME$", providerBank);
        valueMap.put("$BANK_ACCOUNT$", providerBankAccount);

        String orderMessage = orderTemplate.content;
        orderMessage = replacePlaceholders(orderMessage, valueMap);
        String orderSubject = orderTemplate.metadata.get("subject");
        orderSubject = replacePlaceholders(orderSubject, valueMap);

        // send email with order to service admin
        MessageEnvelope envelope = new MessageEnvelope();
        User user = new User();
        user.email = adminEmail;
        envelope.message = orderMessage;
        envelope.subject = orderSubject;
        envelope.user = user;
        messagePort.processAdminEmail(envelope);

        // send email with order to client
        envelope = new MessageEnvelope();
        user = new User();
        user.email = order.email;
        envelope.message = orderMessage;
        envelope.subject = orderSubject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);

        // proforma
        String proformaMessage = proformaTemplate.content;
        proformaMessage = replacePlaceholders(proformaMessage, valueMap);
        String proformaSubject = proformaTemplate.metadata.get("subject");
        proformaSubject = replacePlaceholders(proformaSubject, valueMap);

        // send email with proforma to client
        envelope = new MessageEnvelope();
        user = new User();
        user.email = order.email;
        envelope.message = proformaMessage;
        envelope.subject = proformaSubject;
        envelope.user = user;
        messagePort.processDirectEmail(envelope);

        // send email with proforma to service admin
        envelope = new MessageEnvelope();
        user = new User();
        user.email = adminEmail;
        envelope.message = proformaMessage;
        envelope.subject = proformaSubject;
        envelope.user = user;
        messagePort.processAdminEmail(envelope);

    }

    /**
     * Finds all words starting with { and ending with }.
     * 
     * @param text
     * @return list of words
     */
    private String[] findPlaceholders(String text) {
        // return text.split("\\{(?:...)\\}");
        /* Pattern p = Pattern.compile("\\{(?:...)\\}");
        Matcher m = p.matcher(text);
        List<String> res = new ArrayList<>();
        while (m.find()) {
            res.add(m.group(1));
        }
        return res.toArray(new String[0]); */
        //Pattern pattern = Pattern.compile("\\{[^}]*\\}");
        Pattern pattern = Pattern.compile("\\$[^$]*\\$");
        Matcher matcher = pattern.matcher(text);
        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            results.add(matcher.group());
        }
        return results.toArray(new String[0]);
    }

    /**
     * Replaces placeholders in the text with values and User fields.
     * 
     * @param text
     * @param user
     * @param valueMap
     * @return
     */
    private String replacePlaceholders(String text, HashMap<String, String> valueMap) {
        String result = text;
        String[] placeholders = findPlaceholders(text);
        String key;
        for (int i = 0; i < placeholders.length; i++) {
            logger.info("Placeholder: [" + placeholders[i] + "]");
            try {
                key = placeholders[i];
                if (valueMap.containsKey(key)) {
                    result = result.replace(key, valueMap.get(key));
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
        return result;
    }

    private  String formatTimestampToLocalDate(Timestamp timestamp) {
        // Konwersja Timestamp na LocalDate
        LocalDate localDate = timestamp.toInstant().atZone(ZoneId.of("Europe/Warsaw")).toLocalDate();
        
        // Definiowanie formatu daty
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // Można dostosować wzorzec formatu
        
        // Formatowanie i zwracanie daty jako String
        return localDate.format(formatter);
    }

    private String formatCurrency(double amount, Locale locale) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        return currencyFormatter.format(amount);
    }

}
