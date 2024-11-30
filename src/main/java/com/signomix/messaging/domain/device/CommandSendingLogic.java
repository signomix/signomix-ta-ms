package com.signomix.messaging.domain.device;

import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.iot.Device;
import com.signomix.common.tsdb.IotDatabaseDao;
import com.signomix.messaging.application.port.in.ForCommandSendingIface;
import com.signomix.messaging.domain.Message;
import com.signomix.messaging.webhook.WebhookService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import java.util.ArrayList;
import java.util.Base64;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CommandSendingLogic implements ForCommandSendingIface {

    @Inject
    Logger logger;

    @Inject
    @DataSource("oltp")
    AgroalDataSource dataSource;

    IotDatabaseIface dao;

    void onStart(@Observes StartupEvent ev) {
        dao = new IotDatabaseDao();
        dao.setDatasource(dataSource);
    }

    /**
     * Send waiting commands to devices
     * 
     * Prerequisites:
     * - The device must be of type "TTN"
     * - The device must have a downlink key set in device.downlink
     * - The device must have a device ID set in device.deviceID
     * - The device must have an application ID set in device.applicationID
     * 
     * @param bytes
     */
    public void sendWaitingCommands(byte[] bytes) {
        logger.info("Sending waiting commands: " + new String(bytes));
        // get waiting commands from database for every device
        String sql = "SELECT id,origin, type, payload, createdat " +
                "FROM (" +
                "SELECT id,origin, type, payload, createdat, ROW_NUMBER() OVER (PARTITION BY origin ORDER BY createdat DESC) AS rn FROM commands"
                +
                ") subquery " +
                "WHERE rn = 1;";
        ArrayList<CommandDto> commands = new ArrayList<>();
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // send commands to devices
                CommandDto command = new CommandDto();
                command.id = rs.getLong("id");
                command.deviceEui = rs.getString("origin");
                command.type = rs.getString("type");
                command.payload = rs.getString("payload");
                command.createdAt = rs.getLong("createdat");
                commands.add(command);
            }
        } catch (Exception e) {
            logger.error("Error reading waiting commands: " + e.getMessage());
        }
        // send commands to devices
        Device device = null;
        boolean success = false;
        for (CommandDto command : commands) {
            logger.info("Sending command to device: " + command.deviceEui);
            success = false;
            try {
                device = dao.getDevice(command.deviceEui, false);
                String messageBody = encodeCommand(command.payload, command.type);
                if (device.getType().equalsIgnoreCase("TTN")) {
                    String deviceId = device.getDeviceID();
                    String applicationId = device.getApplicationID();
                    String downlinkKey = device.getDownlink();
                    if (deviceId == null || applicationId == null || downlinkKey == null
                            || deviceId.isEmpty() || applicationId.isEmpty() || downlinkKey.isEmpty()) {
                        logger.warn(
                                "Device " + command.deviceEui
                                        + " is missing device ID, application ID or downlink key");
                        // TODO: inform user
                        continue;
                    }
                    // send command to device
                    // only TTN community network is supported
                    sendTtnDownlink(applicationId, deviceId, downlinkKey, messageBody);
                    success = true;
                } else if (device.getType().equalsIgnoreCase("CHIRPSTACK")) {
                    logger.warn("Device " + command.deviceEui +" - downlinks to ChirpStack not supported");
                } else {
                    logger.warn("Device " + command.deviceEui + " is not of type TTN");
                }
                if (success) {
                    dao.putCommandLog(command.id, command.deviceEui, command.type, command.payload, command.createdAt);
                    dao.removeCommand(command.id);
                } else {
                    // TODO: commandslog table must be updated with the status and error message
                    dao.putCommandLog(command.id, command.deviceEui, command.type, command.payload, command.createdAt);
                    dao.removeCommand(command.id);
                }
            } catch (IotDatabaseException e) {
                logger.error("Error sending command: " + e.getMessage());
                e.printStackTrace();
            }
            // send command to device
        }
    }

    /**
     * Encode command to Base64
     * Only ACTUATOR_HEXCMD is supported
     * 
     */
    private String encodeCommand(String payload, String type) {
        logger.info("Encoding command: " + payload + " type " + type);
        String encoded = null;
        if (type.equalsIgnoreCase("ACTUATOR_HEXCMD")) {
            // convert hex command to Base64
            byte[] byteArray = hexStringToByteArray(payload);
            encoded = byteArrayToBase64(byteArray);
            // encoded = java.util.Base64.getEncoder()
            // .encodeToString(javax.xml.bind.DatatypeConverter.parseHexBinary(payload));
        } else if (type.equalsIgnoreCase("ACTUATOR_PLAINCMD")) {
            // convert plain command to Base64
            // payload = java.util.Base64.getEncoder().encodeToString(payload.getBytes());
        } else if (type.equalsIgnoreCase("ACTUATOR_CMD")) {
            // payload is a JSON string
            // payload = java.util.Base64.getEncoder().encodeToString(payload.getBytes());
        } else {
            logger.warn("Unknown command type: " + type);
        }
        logger.info("Encoded command: " + encoded);
        return encoded;
    }

    private void sendTtnDownlink(String applicationId, String deviceId, String downlinkKey, String payload) {
        if (payload == null || payload.isEmpty()) {
            logger.warn("Empty message body");
            return;
        }
        // send command to device
        String url = "https://eu1.cloud.thethings.network/api/v3/as/applications/" + applicationId + "/devices/"
                + deviceId + "/down/push";
        String priority = "NORMAL";
        int fport = 1;
        String headerName = "Authorization";
        String headerValue = "Bearer " + downlinkKey;
        String message = "{\"downlinks\":[{\"f_port\":" + fport + ",\"frm_payload\":\"" + payload + "\",\"priority\":\""
                + priority + "\"}]}";
        new WebhookService().send(
                url,
                new Message("", message, ""),
                headerName, headerValue);
    }

    // Convert hex string to byte array
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    // Convert byte array to base64 string
    private String byteArrayToBase64(byte[] byteArray) {
        String logMsg = "";
        for (int i = 0; i < byteArray.length; i++) {
            logMsg = logMsg + byteArray[i] + " ";
        }
        logger.info("byteArrayToBase64 bytes: " + logMsg);
        return Base64.getEncoder().encodeToString(byteArray);
    }

    /*
     * Example of a downlink request to TTN:
     * curl -i --location \
     * --header 'Authorization: Bearer NNSXS.XXXXXXXXX' \
     * --header 'Content-Type: application/json' \
     * --header 'User-Agent: signomix' \
     * --request POST \
     * --data '{"downlinks":[{
     * "frm_payload":"vu8=",
     * "f_port":1,
     * "priority":"NORMAL"
     * }]
     * }' \
     * 'https://eu1.cloud.thethings.network/api/v3/as/applications/application-ID/
     * devices/device-ID/down/push'
     */

}
