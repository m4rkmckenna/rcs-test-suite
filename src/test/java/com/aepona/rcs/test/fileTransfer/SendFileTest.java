package com.aepona.rcs.test.fileTransfer;

import java.util.Properties;

import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.aepona.rcs.test.common.TestSubscriber;
import com.aepona.rcs.test.common.TestUtils;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring/application-context.xml"})
public class SendFileTest {

    private final Logger LOGGER = LoggerFactory.getLogger(RetrieveListTest.class);

    @Value("${proxyURL}")
    protected String proxyURL;

    @Value("${proxyPort}")
    protected String proxyPort;

    @Value("${baseURI}")
    protected String baseURI;

    @Value("${apiVersion}")
    protected String apiVersion;

    @Value("${port}")
    protected int port;

    @Value("${applicationUsername}")
    protected String applicationUsername;

    @Value("${applicationPassword}")
    protected String applicationPassword;

    @Value("${user1}")
    protected String user1;

    @Value("${user2}")
    protected String user2;

    @Value("${user3}")
    protected String user3;

    @Value("${invalidUser}")
    protected String invalidUser;

    @Value("${contact1}")
    protected String contact1;

    @Value("${contact2}")
    protected String contact2;

    @Value("${invalidContact}")
    protected String invalidContact;

    @Value("${registerURL}")
    protected String registerURL;

    @Value("${sessionRequestData}")
    protected String sessionRequestData;

    @Value("${sessionSubscriptionURL}")
    protected String sessionSubscriptionURL;

    @Value("${validLongPoll}")
    protected String validLongPoll;

    @Value("${notificationChannelURL}")
    protected String notificationChannelURL;

    @Value("${addressBookRequestData}")
    protected String addressBookRequestData;

    @Value("${addressBookSubscriptionURL}")
    protected String addressBookSubscriptionURL;

    @Value("${fileTransferRequestData}")
    protected String fileTransferRequestData;

    @Value("${fileTransferSubscriptionURL}")
    protected String fileTransferSubscriptionURL;

    @Value("${urlSplit}")
    protected String urlSplit;

    @Value("${sendFileURL}")
    protected String sendFileURL;

    @Value("${fileTransferStatusURL}")
    protected String fileTransferStatusURL;

    @Value("${fileTransferSessionURL}")
    protected String fileTransferSessionURL;

    String lastTest = null;

    Boolean initialised = false;

    private String senderSessionID;

    private String recipientSessionID;

    private String attachmentURL;

    private String savedResourceURL;

    private TestSubscriber userOne;

    private TestSubscriber userTwo;

    @Before
    public void setup() {
        LOGGER.info("Proxy URL: " + proxyURL);
        Properties props = System.getProperties();
        props.put("http.proxyHost", proxyURL);
        props.put("http.proxyPort", proxyPort);
        start();
        userOne = new TestSubscriber();
        userOne.setUserID(user1);
        userTwo = new TestSubscriber();
        userTwo.setUserID(user2);
        initialiseUser(userOne);
        initialiseUser(userTwo);
    }

    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(userOne, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userTwo, applicationUsername, applicationPassword);
    }

    @Test
    public void sendFileURLPointer() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        int i = 1;
        String contactSIP = contact2;
        int j = 2;

        String test = "Sending File (URL Pointer) from User " + i + " to User " + j;
        startTest(test);

        String requestData =
                "{\"fileTransferSessionInformation\": {\"fileInformation\": {\"fileDescription\": \"This is my latest picture\",\"fileDisposition\": \"Render\","
                        + "\"fileSelector\": {\"name\": \"tux.png\",\"size\": 56320,\"type\": \"image/png\"},\"fileURL\": \"http://tux.crystalxp.net/png/brightknight-doraemon-tux-3704.png\"},"
                        + "\"originatorAddress\": \""
                        + userSIP
                        + "\",\"originatorName\": \"G3\",\"receiverAddress\": \""
                        + contactSIP
                        + "\",\"receiverName\": \"G4\"}}";
        String url = replace(sendFileURL, apiVersion, userID);
        LOGGER.info("Request Data = " + requestData);
        LOGGER.info("URL = " + baseURI + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(requestData)
                           .expect()
                           .statusCode(201)
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        String resourceURL = response.jsonPath().getString("resourceReference.resourceURL");
        String[] parts = resourceURL.split("/sessions/");
        senderSessionID = parts[i];
        LOGGER.info("Sender Session ID = " + senderSessionID);
        endTest(test);
    }

    @Test
    public void checkingNotifications() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        sleep();
        sleep();
        String test = "Checking if User " + userTwo + " received any Notifications";
        startTest(test);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .urlEncodingEnabled(true)
                           .expect()
                           .statusCode(200)
                           .body("notificationList.ftSessionInvitationNotification[0].fileInformation.resourceURL",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].sessionId",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].originatorAddress",
                                 Matchers.containsString(cleanUserID),
                                 "notificationList.ftSessionInvitationNotification[0].receiverName",
                                 Matchers.equalTo(contactID),
                                 "notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.name",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.type",
                                 Matchers.equalTo("image/png"))
                           .post(userTwo.getChannelURL());

        // recipientSessionID =
        // response.jsonPath().getString("notificationList.ftSessionInvitationNotification[0].sessionId");
        // attachmentURL =
        // response.jsonPath().getString("notificationList.ftSessionInvitationNotification[0].fileInformation.fileURL");
        //
        // LOGGER.info("Response Received = "+response.getStatusCode());
        // LOGGER.info("Receiver Session = "+recipientSessionID);
        // LOGGER.info("File Name = "+response.jsonPath().getString("notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.name"));
        // LOGGER.info("Attachment URL = "+attachmentURL);
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void acceptTransfer() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        checkNotificationsForTransfer(contactID, cleanUserID, userTwo);

        String test = "Accepting Transfer from User " + userOne;
        startTest(test);
        String sessionID = recipientSessionID;
        LOGGER.info("***" + sessionID);
        String acceptData = "{\"receiverSessionStatus\": {\"status\": \"Connected\"}}";
        String url = replaceLong(fileTransferStatusURL, apiVersion, contactID, sessionID);
        LOGGER.info("URL : " + url);
        LOGGER.info("Request Data : " + acceptData);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(acceptData)
                           .urlEncodingEnabled(true)
                           .expect()
                           .statusCode(204)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void declineTransfer() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        checkNotificationsForTransfer(contactID, cleanUserID, userTwo);

        String test = "Declining Transfer from User " + userOne;
        startTest(test);
        String sessionID = recipientSessionID;
        LOGGER.info("***" + sessionID);
        String url = replaceLong(fileTransferSessionURL, apiVersion, contactID, sessionID);
        LOGGER.info("URL = " + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .delete(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void checkingSenderNotifications() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        checkNotificationsForTransfer(contactID, cleanUserID, userTwo);
        sleep();
        acceptTransfer(contactID, userOne);

        String test = "Checking if User " + userOne + " received any Notifications";
        startTest(test);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .urlEncodingEnabled(true)
                           .expect()
                           .statusCode(200)
                           .body("notificationList.receiverAcceptanceNotification[0].receiverSessionStatus.status",
                                 Matchers.equalTo("Connected"),
                                 "notificationList.receiverAcceptanceNotification[0].sessionId",
                                 Matchers.equalTo(senderSessionID))
                           .post(userOne.getChannelURL());

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void gettingFileURL() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        checkNotificationsForTransfer(contactID, cleanUserID, userTwo);
        sleep();
        acceptTransfer(contactID, userOne);

        String test = "Getting the File URL...";
        startTest(test);

        LOGGER.info("URL = " + userTwo.getChannelURL());
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .statusCode(200)
                           .body("notificationList", Matchers.notNullValue())
                           .post(userTwo.getChannelURL());

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        attachmentURL = response.jsonPath().getString("notificationList.fileNotification[0].fileInformation.fileURL");
        LOGGER.info("Attachment URL = " + attachmentURL);
        endTest(test);
    }

    @Test
    public void receiveAttachment() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactID = user2;
        String contactSIP = contact2;

        sendFilePointer(userID, userSIP, contactSIP);
        sleep();
        checkNotificationsForTransfer(contactID, cleanUserID, userTwo);
        sleep();
        acceptTransfer(contactID, userOne);
        sleep();
        getFileURL(contactID, userTwo);

        String test = "Receiving attachment...";
        startTest(test);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .urlEncodingEnabled(false)
                           .expect()
                           .statusCode(200)
                           .get(attachmentURL);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Content Type = " + response.getContentType());
        LOGGER.info("Content disposition = " + response.getHeader("Content-Disposition"));
        LOGGER.info("Content length = " + response.getHeader("Content-Length"));
        endTest(test);
    }

    @Test
    public void readFileTransferSession() {
        String userID = user1;
        String userSIP = contact1;
        String cleanUserID = cleanPrefix(userID);
        String contactSIP = contact2;

        String test = "Creating session that will be read...";
        String requestData =
                "{\"fileTransferSessionInformation\": {\"fileInformation\": {\"fileDescription\": \"This is my latest picture\",\"fileDisposition\": \"Render\","
                        + "\"fileSelector\": {\"name\": \"tux.png\",\"size\": 56320,\"type\": \"image/png\"},\"fileURL\": \"http://tux.crystalxp.net/png/brightknight-doraemon-tux-3704.png\"},"
                        + "\"originatorAddress\": \""
                        + userSIP
                        + "\",\"originatorName\": \"G1\",\"receiverAddress\": \""
                        + contactSIP
                        + "\",\"receiverName\": \"G2\"}}";
        String url = replace(sendFileURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(requestData)
                           .expect()
                           .statusCode(201)
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        savedResourceURL = response.jsonPath().getString("resourceReference.resourceURL");
        LOGGER.info("Message resourceURL = " + savedResourceURL);
        endTest(test);

        url = prepare(savedResourceURL);
        test = "Reading Session Information URL " + url;
        startTest(test);

        response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .statusCode(200)
                           .body("fileTransferSessionInformation.resourceURL",
                                 StringContains.containsString(cleanUserID), "fileTransferSessionInformation.status",
                                 Matchers.equalTo("Invited"))
                           .get(url);
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

    }

    @Test
    public void mismatchedSenderInURL() {
        String userSIP = contact1;
        String contactSIP = contact2;
        String mismatchedSender = user3;

        String test = "Error Test = Mismatched Sender Address";
        startTest(test);

        String requestData =
                "{\"fileTransferSessionInformation\": {\"fileInformation\": {\"fileDescription\": \"This is my latest picture\",\"fileDisposition\": \"Render\","
                        + "\"fileSelector\": {\"name\": \"tux.png\",\"size\": 56320,\"type\": \"image/png\"},\"fileURL\": \"http://tux.crystalxp.net/png/brightknight-doraemon-tux-3704.png\"},"
                        + "\"originatorAddress\": \""
                        + userSIP
                        + "\",\"originatorName\": \"G1\",\"receiverAddress\": \""
                        + contactSIP
                        + "\",\"receiverName\": \"G2\"}}";
        String url = replace(sendFileURL, apiVersion, mismatchedSender);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(requestData)
                           .expect()
                           .statusCode(400)
                           .body("requestError.serviceException.messageId", Matchers.equalTo("SVC002"),
                                 "requestError.serviceException.variables",
                                 Matchers.hasItem("Originator's Address is wrong"))
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Error Message = " + response.jsonPath().getString("requestError.serviceException.variables[0]"));
        endTest(test);
    }

    @Test
    public void checkNoNotifications() {
        String userSIP = contact1;
        String contactSIP = contact2;
        String mismatchedSender = user3;

        mismatchedSender(userSIP, mismatchedSender, contactSIP);
        sleep();

        String test = "Checking that User " + userOne + " has no notifications";
        startTest(test);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .statusCode(200)
                           .body("notificationList", Matchers.nullValue())
                           .post(userOne.getChannelURL());

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

        test = "Checking that User " + userTwo + " has no notifications";
        startTest(test);

        response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .statusCode(200)
                           .body("notificationList", Matchers.nullValue())
                           .post(userTwo.getChannelURL());

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    // *** Helper Methods ***
    private void mismatchedSender(String userSIP, String mismatchedSender, String contactSIP) {
        String test = "Error Test = Mismatched Sender Address";
        startTest(test);

        String requestData =
                "{\"fileTransferSessionInformation\": {\"fileInformation\": {\"fileDescription\": \"This is my latest picture\",\"fileDisposition\": \"Render\","
                        + "\"fileSelector\": {\"name\": \"tux.png\",\"size\": 56320,\"type\": \"image/png\"},\"fileURL\": \"http://tux.crystalxp.net/png/brightknight-doraemon-tux-3704.png\"},"
                        + "\"originatorAddress\": \""
                        + userSIP
                        + "\",\"originatorName\": \"G1\",\"receiverAddress\": \""
                        + contactSIP
                        + "\",\"receiverName\": \"G2\"}}";
        String url = replace(sendFileURL, apiVersion, mismatchedSender);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(requestData)
                           .expect()
                           .statusCode(400)
                           .body("requestError.serviceException.messageId", Matchers.equalTo("SVC002"),
                                 "requestError.serviceException.variables",
                                 Matchers.hasItem("Originator's Address is wrong"))
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Error Message = " + response.jsonPath().getString("requestError.serviceException.variables[0]"));
        endTest(test);
    }

    private void getFileURL(String contactID, final TestSubscriber testSubscriber) {
        String test = "Getting the File URL...";
        startTest(test);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .statusCode(200)
                           .body("notificationList", Matchers.notNullValue())
                           .post(testSubscriber.getChannelURL());

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        attachmentURL = response.jsonPath().getString("notificationList.fileNotification[0].fileInformation.fileURL");
        LOGGER.info("Attachment URL = " + attachmentURL);
        endTest(test);

    }

    private void acceptTransfer(String contactID, final TestSubscriber testSubscriber) {
        String test = "Accepting Transfer from User " + testSubscriber;
        startTest(test);
        String sessionID = recipientSessionID;
        LOGGER.info("***" + sessionID);
        String acceptData = "{\"receiverSessionStatus\": {\"status\": \"Connected\"}}";
        String url = replaceLong(fileTransferStatusURL, apiVersion, contactID, sessionID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(acceptData)
                           .urlEncodingEnabled(true)
                           .expect()
                           .statusCode(204)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        endTest(test);
    }

    private void checkNotificationsForTransfer(String contactID, String cleanUserID, final TestSubscriber testSubscriber) {
        String test = "Checking if User " + testSubscriber + " received any Notifications";
        startTest(test);

        LOGGER.info("Notification URL = " + testSubscriber.getChannelURL());
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .urlEncodingEnabled(true)
                           .expect()
                           .statusCode(200)
                           .body("notificationList.ftSessionInvitationNotification[0].fileInformation.resourceURL",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].sessionId",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].originatorAddress",
                                 Matchers.containsString(cleanUserID),
                                 "notificationList.ftSessionInvitationNotification[0].receiverName",
                                 Matchers.equalTo(contactID),
                                 "notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.name",
                                 Matchers.notNullValue(),
                                 "notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.type",
                                 Matchers.equalTo("image/png"))
                           .post(testSubscriber.getChannelURL());

        recipientSessionID =
                response.jsonPath().getString("notificationList.ftSessionInvitationNotification[0].sessionId");
        attachmentURL =
                response.jsonPath()
                        .getString("notificationList.ftSessionInvitationNotification[0].fileInformation.fileURL");

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Receiver Session = " + recipientSessionID);
        LOGGER.info("File Name = "
                + response.jsonPath()
                          .getString("notificationList.ftSessionInvitationNotification[0].fileInformation.fileSelector.name"));
        LOGGER.info("Attachment URL = " + attachmentURL);
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    private void sendFilePointer(String userID, String userSIP, String contactSIP) {
        String cleanUserID = cleanPrefix(userID);

        String test = "Sending File (URL Pointer) from User " + userOne + " to User " + userTwo;
        startTest(test);

        String requestData =
                "{\"fileTransferSessionInformation\": {\"fileInformation\": {\"fileDescription\": \"This is my latest picture\",\"fileDisposition\": \"Render\","
                        + "\"fileSelector\": {\"name\": \"tux.png\",\"size\": 56320,\"type\": \"image/png\"},\"fileURL\": \"http://tux.crystalxp.net/png/brightknight-doraemon-tux-3704.png\"},"
                        + "\"originatorAddress\": \""
                        + userSIP
                        + "\",\"originatorName\": \"G3\",\"receiverAddress\": \""
                        + contactSIP
                        + "\",\"receiverName\": \"G4\"}}";
        String url = replace(sendFileURL, apiVersion, userID);

        LOGGER.info("Sending body = " + requestData);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(requestData)
                           .expect()
                           .statusCode(201)
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        String resourceURL = response.jsonPath().getString("resourceReference.resourceURL");
        String[] parts = resourceURL.split("/sessions/");
        senderSessionID = parts[1];
        LOGGER.info("Sender Session ID = " + senderSessionID);
        endTest(test);
    }

    // *** General Methods ***
    private void sleep() {
        try {
            LOGGER.info("Waiting........");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (!initialised) {
            RestAssured.baseURI = baseURI;
            RestAssured.port = port;
            RestAssured.basePath = "";
            RestAssured.urlEncodingEnabled = true;
            initialised = true;
        }
    }

    public void initialiseUser(final TestSubscriber testUser) {
        LOGGER.info("Initialising User " + testUser + "");
        TestUtils.registerUser(testUser, registerURL, apiVersion, applicationUsername, applicationPassword);
        TestUtils.startNotificationChannel(testUser, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToFileTransfer(testUser, fileTransferSubscriptionURL, fileTransferRequestData, apiVersion,
                                          applicationUsername, applicationPassword);
        TestUtils.clearPendingNotifications(testUser, applicationUsername, applicationPassword);
        LOGGER.info("User " + testUser + " has been Initalised!");
    }

    private void startTest(String test) {
        if (lastTest != null) {
            LOGGER.info("Ending the test: '" + lastTest + "' premeturely...");
        }
        LOGGER.info("Starting the test: '" + test + "'");
    }

    private void endTest(String test) {
        LOGGER.info("End of test: '" + test + "'");
    }

    private String replace(String chatSubscriptionURL, String apiVersion, String userID) {
        return chatSubscriptionURL.replace("{apiVersion}", apiVersion).replace("{userID}", userID);
    }

    private String replaceLong(String url, String apiVersion, String userID, String sessionID) {
        return url.replace("{apiVersion}", apiVersion).replace("{userID}", userID).replace("{sessionID}", sessionID);
    }

    public String cleanPrefix(String userID) {
        return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "").replaceAll("\\+", "").replaceAll("\\:", "");
    }

    private String prepare(String url) {
        return url.replaceAll(urlSplit, "").replace("%2B", "+").replace("%3A", ":");
    }

    public void setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setApplicationPassword(String applicationPassword) {
        this.applicationPassword = applicationPassword;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public void setUser3(String user3) {
        this.user3 = user3;
    }

    public void setInvalidUser(String invalidUser) {
        this.invalidUser = invalidUser;
    }

    public void setContact1(String contact1) {
        this.contact1 = contact1;
    }

    public void setContact2(String contact2) {
        this.contact2 = contact2;
    }

    public void setInvalidContact(String invalidContact) {
        this.invalidContact = invalidContact;
    }

    public void setRegisterURL(String registerURL) {
        this.registerURL = registerURL;
    }

    public void setSessionRequestData(String sessionRequestData) {
        this.sessionRequestData = sessionRequestData;
    }

    public void setSessionSubscriptionURL(String sessionSubscriptionURL) {
        this.sessionSubscriptionURL = sessionSubscriptionURL;
    }

    public void setValidLongPoll(String validLongPoll) {
        this.validLongPoll = validLongPoll;
    }

    public void setNotificationChannelURL(String notificationChannelURL) {
        this.notificationChannelURL = notificationChannelURL;
    }

    public void setAddressBookRequestData(String addressBookRequestData) {
        this.addressBookRequestData = addressBookRequestData;
    }

    public void setAddressBookSubscriptionURL(String addressBookSubscriptionURL) {
        this.addressBookSubscriptionURL = addressBookSubscriptionURL;
    }

    public void setFileTransferRequestData(String fileTransferRequestData) {
        this.fileTransferRequestData = fileTransferRequestData;
    }

    public void setFileTransferSubscriptionURL(String fileTransferSubscriptionURL) {
        this.fileTransferSubscriptionURL = fileTransferSubscriptionURL;
    }

    public void setUrlSplit(String urlSplit) {
        this.urlSplit = urlSplit;
    }

    public void setSendFileURL(String sendFileURL) {
        this.sendFileURL = sendFileURL;
    }

    public void setFileTransferStatusURL(String fileTransferStatusURL) {
        this.fileTransferStatusURL = fileTransferStatusURL;
    }

    public void setFileTransferSessionURL(String fileTransferSessionURL) {
        this.fileTransferSessionURL = fileTransferSessionURL;
    }

}
