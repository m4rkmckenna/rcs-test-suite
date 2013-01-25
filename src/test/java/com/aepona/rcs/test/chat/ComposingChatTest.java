package com.aepona.rcs.test.chat;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.Matchers;
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
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/application-context.xml" })
public class ComposingChatTest {

    private final Logger LOGGER = LoggerFactory.getLogger(ComposingChatTest.class);

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

    @Value("${contact1}")
    protected String contact1;

    @Value("${contact2}")
    protected String contact2;

    @Value("${chatSessionIMStatusURL}")
    protected String chatSessionIMStatusURL;

    @Value("${chatSessionIMURL}")
    protected String chatSessionIMURL;

    @Value("${sendIMURL}")
    protected String sendIMURL;

    @Value("${createIMChatSessionURL}")
    protected String createIMChatSessionURL;

    @Value("${chatRequestDataConfirmed}")
    protected String chatRequestDataConfirmed;

    @Value("${chatSubscriptionURL}")
    protected String chatSubscriptionURL;

    @Value("${notificationChannelURL}")
    protected String notificationChannelURL;

    @Value("${validLongPoll}")
    protected String validLongPoll;

    @Value("${registerURL}")
    protected String registerURL;

    @Value("${sessionRequestData}")
    protected String sessionRequestData;

    @Value("${sessionSubscriptionURL}")
    protected String sessionSubscriptionURL;

    @Value("${addressBookSubscriptionURL}")
    protected String addressBookSubscriptionURL;

    @Value("${addressBookRequestData}")
    protected String addressBookRequestData;

    String lastTest = null;

    Boolean initialised = false;

    private TestSubscriber userOne;

    private TestSubscriber userTwo;

    private String sentMessageID;

    private String receiveMessageID;

    private String receiveSessionID;

    private String sendSessionURL;

    private String receiveSessionURL;

    private String sessionID;

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

    public void initialiseUser(final TestSubscriber testUser) {
        LOGGER.info("Initialising User " + testUser);
        TestUtils.registerUser(testUser, registerURL, apiVersion, applicationUsername, applicationPassword);
        TestUtils.startNotificationChannel(testUser, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToChatNotifications(testUser, chatSubscriptionURL, chatRequestDataConfirmed,
                                                    apiVersion, applicationUsername, applicationPassword);
        TestUtils.clearPendingNotifications(testUser, applicationUsername, applicationPassword);
        LOGGER.info("User 1 has been Initalised!");
    }

    @Test
    public void sendIsComposingA1() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        // Setup
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);

        String test =
                "Starting send isComposing functionality between User (" + sender + ") and Contact (" + receiver
                        + ") - ACTIVE";
        startTest(test);

        String state = "active";
        Date lastActive = new java.util.Date();
        String contentType = "test/plain";
        int refresh = 60;
        userID = user1;
        contactID = contact2;
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, sessionID);
        IsComposing isComposing = new IsComposing(state, lastActive, contentType, refresh);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"isComposing\":" + mapper.writeValueAsString(isComposing) + "}";

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void checkNotificationsForComposingA1() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "active";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();

        String test = "Checking that User (" + receiver + ") Received 'composing' Notification - ACTIVE";
        startTest(test);
        userID = user2;
        contactID = contact1;
        String url = userTwo.getChannelURL();
        String cleanContact1 = cleanPrefix(contact1);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.messageNotification[0].isComposing.refresh", Matchers.equalTo(60),
                                 "notificationList.messageNotification[0].isComposing.status",
                                 Matchers.equalTo("active"), "notificationList.messageNotification[0].sessionId",
                                 Matchers.equalTo(receiveSessionID),
                                 "notificationList.messageNotification[0].senderAddress",
                                 Matchers.containsString(cleanContact1))
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void sendIsComposingB1() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "active";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();
        userID = user2;
        contactID = contact1;
        isComposingNotification(userTwo, contactID);
        sleep();
        // Start composing User 2
        startComposing(userID, contactID, state, contentType, receiveSessionID);
    }

    @Test
    public void checkNotificationsForComposingB1() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "active";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        // Send 'isComposing' from 1 - 2
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();
        userID = user2;
        contactID = contact1;
        // 'isComposing' Notification for User 2
        isComposingNotification(userTwo, contactID);
        sleep();
        // Start composing from 2 - 1
        startComposing(userID, contactID, state, contentType, receiveSessionID);
        sleep();
        // 'isComposing' Notification for User 1
        userID = user1;
        contactID = contact2;
        isComposingNotification(userOne, contactID);
    }

    @Test
    public void sendIsComposingA2() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);

        String test =
                "Starting send isComposing functionality between User (" + sender + ") and Contact (" + receiver
                        + ") - IDLE";
        startTest(test);

        String state = "idle";
        Date lastActive = new java.util.Date();
        String contentType = "test/plain";
        int refresh = 60;
        userID = user1;
        contactID = contact2;
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, sessionID);
        IsComposing isComposing = new IsComposing(state, lastActive, contentType, refresh);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"isComposing\":" + mapper.writeValueAsString(isComposing) + "}";

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void checkNotificationsForComposingA2() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "idle";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();

        String test = "Checking that User (" + receiver + ") Received 'composing' Notification - IDLE";
        startTest(test);
        userID = user2;
        contactID = contact1;
        String url = userTwo.getChannelURL();
        String cleanContact1 = cleanPrefix(contact1);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.messageNotification[0].isComposing.refresh", Matchers.equalTo(60),
                                 "notificationList.messageNotification[0].senderAddress",
                                 Matchers.containsString(cleanContact1))
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void sendIsComposingB2() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "idle";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();
        userID = user2;
        contactID = contact1;
        isComposingNotification(userTwo, contactID);
        sleep();
        // Start composing User 2
        startComposing(userID, contactID, state, contentType, receiveSessionID);
    }

    @Test
    public void checkNotificationsForComposingB2() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String state = "idle";
        String contentType = "text/plain";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        userID = user1;
        contactID = contact2;
        // Send 'isComposing' from 1 - 2
        startComposing(userID, contactID, state, contentType, sessionID);
        sleep();
        userID = user2;
        contactID = contact1;
        // 'isComposing' Notification for User 2
        isComposingNotification(userTwo, contactID);
        sleep();
        // Start composing from 2 - 1
        startComposing(userID, contactID, state, contentType, receiveSessionID);
        sleep();
        // 'isComposing' Notification for User 1
        userID = user1;
        contactID = contact2;
        isComposingNotification(userOne, contactID);
    }

    @Test
    public void sendIsComposingA3() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);

        String test =
                "Starting send isComposing functionality between User (" + sender + ") and Contact (" + receiver
                        + ") - INVALID";
        startTest(test);

        String state = "invalid";
        Date lastActive = new java.util.Date();
        String contentType = "test/plain";
        int refresh = 60;
        userID = user1;
        contactID = contact2;
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, sessionID);
        IsComposing isComposing = new IsComposing(state, lastActive, contentType, refresh);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"isComposing\":" + mapper.writeValueAsString(isComposing) + "}";

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(400)
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        String errorCode = jsonData.get("requestError.serviceException.messageId");
        String errorMessage = jsonData.get("requestError.serviceException.variables[0]");

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Error Code = " + errorCode);
        LOGGER.info("Error Message = " + errorMessage);
        endTest(test);
    }

    @Test
    public void closeChat() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);

        String test = "Closing Chat Session between User 1 and Contact 2 - User 2 CLOSING";
        startTest(test);
        String url = replaceExtraLong(chatSessionIMURL, apiVersion, userID, contactID, receiveSessionID);

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
    public void receiveCloseNotification() throws JsonGenerationException, JsonMappingException, IOException {
        // Variables
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userTwo);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(userOne);
        sleep();
        closeChatSession(userID, contactID, receiveSessionID);
        sleep();

        String test = "Checking User 1 for Close Notification";
        startTest(test);

        userID = user1;
        String url = userOne.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.chatEventNotification.eventType", Matchers.hasItem("SessionEnded"),
                                 "notificationList.chatEventNotification.sessionId", Matchers.hasItem(sessionID),
                                 "notificationList.chatEventNotification.link[0].rel",
                                 Matchers.hasItem("ChatSessionInformation"))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Notification Event = "
                + response.jsonPath().getString("notificationList.chatEventNotification.eventType"));
        endTest(test);
    }

    private void closeChatSession(String userID, String contactID, String session) {
        String test = "Closing Chat Session";
        startTest(test);
        String url = replaceExtraLong(chatSessionIMURL, apiVersion, userID, contactID, session);

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

    public void start() {
        if (!initialised) {
            RestAssured.baseURI = baseURI;
            RestAssured.port = port;
            RestAssured.basePath = "";
            RestAssured.urlEncodingEnabled = true;
            initialised = true;
        }
        sentMessageID = null;
        receiveMessageID = null;
        receiveSessionID = null;
        sendSessionURL = null;
        receiveSessionURL = null;
    }

    // ********* METHODS TO PERFORM FUNTIONALITY FOR CHAT CLASS **********
    private void createChatSession(String sender,
                                   String receiver,
                                   String senderSIP,
                                   String subject,
                                   String senderUsername,
                                   String receiverUsername) throws JsonGenerationException, JsonMappingException,
            IOException {
        String test = "Creating Chat Session... " + sender + " (Sender) " + receiver + " (Reciever)";
        startTest(test);
        String endpoint = createIMChatSessionURL;
        String requestData;
        String url = replaceLong(endpoint, apiVersion, sender, receiver);
        ChatSessionInformation chatSessionInformation =
                new ChatSessionInformation(subject, senderSIP, senderUsername, receiver, receiverUsername);
        ObjectMapper mapper = new ObjectMapper();
        requestData = "{\"chatSessionInformation\":" + mapper.writeValueAsString(chatSessionInformation) + "}";
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .post(url);
        JsonPath jsonData = response.jsonPath();
        
        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Resource URL = " + jsonData.getString("chatSessionInformation.resourceURL"));
        endTest(test);
    }

    private void checkIMNotifications(final TestSubscriber testSubscriber) {
        String test = "Checking IM Notifications for User (" + testSubscriber + ")";
        startTest(test);
        String url = testSubscriber.getChannelURL();

        Response notificationResponse =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        JsonPath jsonData = notificationResponse.jsonPath();
        receiveSessionURL = jsonData.getString("notificationList.chatSessionInvitationNotification[0].link[0].href");
        LOGGER.info("Extracted receiveSessionURL=" + receiveSessionURL);
        receiveSessionID = jsonData.getString("notificationList.messageNotification.sessionId[0]");
        LOGGER.info("Extracted receiveSessionID=" + receiveSessionID);
        receiveMessageID = jsonData.getString("notificationList.messageNotification.messageId[0]");
        LOGGER.info("Extracted receiveMessageID=" + receiveMessageID);
        String messageNotification = jsonData.getString("notificationList.messageNotification");
        LOGGER.info("Extracted messageNotification = " + messageNotification);
        endTest(test);
    }

    private void acceptSession(String userID, String contactID) {
        String test = "Accepting Chat Session";
        startTest(test);
        String url = replaceExtraLong(chatSessionIMStatusURL, apiVersion, userID, contactID, receiveSessionID);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .put(url);
        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    private void checkSessionStatusNotification(final TestSubscriber testSubscriber) {
        String test = "Checking Notifications for User (" + testSubscriber + ")";
        startTest(test);
        String url = testSubscriber.getChannelURL();
        Response notificationResponse =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList", Matchers.notNullValue())
                           .post(url);

        JsonPath jsonData = notificationResponse.jsonPath();
        sentMessageID = jsonData.getString("notificationList.messageStatusNotification[0].messageId");
        LOGGER.info("Extracted messageId=" + sentMessageID);
        sendSessionURL = jsonData.getString("notificationList.chatEventNotification.link[0].href[0]");
        LOGGER.info("Extracted sendSessionURL=" + sendSessionURL);
        sessionID = jsonData.getString("notificationList.chatEventNotification.sessionId[0]");
        LOGGER.info("Extracted sessionId=" + sessionID);
        endTest(test);
    }

    private void startComposing(String userID, String contactID, String state, String contentType, String session)
            throws JsonGenerationException, JsonMappingException, IOException {
        String test = "Starting send isComposing functionality...";
        startTest(test);

        Date lastActive = new java.util.Date();
        int refresh = 60;
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, session);
        IsComposing isComposing = new IsComposing(state, lastActive, contentType, refresh);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"isComposing\":" + mapper.writeValueAsString(isComposing) + "}";

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    private void isComposingNotification(final TestSubscriber testSubscriber, String senderContact) {
        String test = "Checking that User (" + testSubscriber + ") Received 'composing' Notification";
        startTest(test);
        String url = testSubscriber.getChannelURL();
        String cleanContact1 = cleanPrefix(senderContact);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.messageNotification[0].isComposing.refresh", Matchers.equalTo(60),
                                 "notificationList.messageNotification[0].senderAddress",
                                 Matchers.containsString(cleanContact1))
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        // LOGGER.info("Body = " + response.asString());
        LOGGER.info("isComposing Status = "
                + response.jsonPath().getString("notificationList.messageNotification[0].isComposing.status"));
        endTest(test);
    }

    // ********* GENERAL METHODS **********
    private void sleep() {
        try {
            LOGGER.info("Waiting......");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    private String replaceLong(String url, String apiVersion, String userID, String contactID) {
        return url.replace("{apiVersion}", apiVersion).replace("{userID}", userID).replace("{contactID}", contactID);
    }

    private String replaceExtraLong(String url, String apiVersion, String userID, String contactID, String sessionID) {
        LOGGER.info(" >>> replaceExtraLong");
        LOGGER.info("url = " + url);
        LOGGER.info("apiVersion = " + apiVersion);
        LOGGER.info("userID = " + userID);
        LOGGER.info("contactID = " + contactID);
        LOGGER.info("sessionID = " + sessionID);
        return url.replace("{apiVersion}", apiVersion)
                  .replace("{userID}", userID)
                  .replace("{contactID}", contactID)
                  .replace("{sessionID}", sessionID);
    }

    public String encode(String userID) {
        return userID.replaceAll("\\:", "%3A").replaceAll("\\+", "%2B").replaceAll("\\@", "%40");
    }

    public String cleanPrefix(String userID) {
        return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "").replaceAll("\\+", "").replaceAll("\\:", "");
    }

    // ******** REQUIRED CLASSES *********
    public class ChatMessage {

        String text;

        String reportRequest;

        String resourceURL;

        public String getText() {
            return text;
        }

        public String getReportRequest() {
            return reportRequest;
        }

        @JsonIgnore
        public String getResourceURL() {
            return resourceURL;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setReportRequest(String reportRequest) {
            this.reportRequest = reportRequest;
        }

        public void setResourceURL(String resourceURL) {
            this.resourceURL = resourceURL;
        }

        public ChatMessage(String text, String reportRequest) {
            this.text = text;
            this.reportRequest = reportRequest;
        }
    }

    public class ChatSessionInformation {

        String subject;

        String originatorAddress;

        String originatorName;

        String tParticipantAddress;

        String tParticipantName;

        String status;

        String resourceURL;

        public String getSubject() {
            return subject;
        }

        public String getOriginatorAddress() {
            return originatorAddress;
        }

        public String getOriginatorName() {
            return originatorName;
        }

        public String gettParticipantAddress() {
            return tParticipantAddress;
        }

        public String gettParticipantName() {
            return tParticipantName;
        }

        @JsonIgnore
        public String getStatus() {
            return status;
        }

        @JsonIgnore
        public String getResourceURL() {
            return resourceURL;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void setOriginatorAddress(String originatorAddress) {
            this.originatorAddress = originatorAddress;
        }

        public void setOriginatorName(String originatorName) {
            this.originatorName = originatorName;
        }

        public void settParticipantAddress(String tParticipantAddress) {
            this.tParticipantAddress = tParticipantAddress;
        }

        public void settParticipantName(String tParticipantName) {
            this.tParticipantName = tParticipantName;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setResourceURL(String resourceURL) {
            this.resourceURL = resourceURL;
        }

        public ChatSessionInformation(String subject,
                                      String originatorAddress,
                                      String originatorName,
                                      String tParticipantAddress,
                                      String tParticipantName) {
            this.subject = subject;
            this.originatorAddress = originatorAddress;
            this.originatorName = originatorName;
            this.tParticipantAddress = tParticipantAddress;
            this.tParticipantName = tParticipantName;
        }
    }

    public class IsComposing {

        String state;

        java.util.Date lastActive;

        String contentType;

        int refresh;

        boolean outputISO8601 = false;

        public String getState() {
            return state;
        }

        public String getLastActive() {
            String dt = null;
            if (lastActive != null) {
                TimeZone zone = TimeZone.getTimeZone("UTC");
                Calendar c = Calendar.getInstance(zone);
                c.setTime(lastActive);
                dt = DatatypeConverter.printDateTime(c).substring(0, 19) + "Z";
            }
            return dt;
        }

        public String getContentType() {
            return contentType;
        }

        public int getRefresh() {
            return refresh;
        }

        public void setState(String state) {
            this.state = state;
        }

        public void setLastActive(java.util.Date lastActive) {
            this.lastActive = lastActive;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setRefresh(int refresh) {
            this.refresh = refresh;
        }

        public IsComposing(String state, java.util.Date lastActive, String contentType, int refresh) {
            this.state = state;
            this.lastActive = lastActive;
            this.contentType = contentType;
            this.refresh = refresh;
        }
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

    public void setContact1(String contact1) {
        this.contact1 = contact1;
    }

    public void setContact2(String contact2) {
        this.contact2 = contact2;
    }

    public void setChatSessionIMStatusURL(String chatSessionIMStatusURL) {
        this.chatSessionIMStatusURL = chatSessionIMStatusURL;
    }

    public void setChatSessionIMURL(String chatSessionIMURL) {
        this.chatSessionIMURL = chatSessionIMURL;
    }

    public void setSendIMURL(String sendIMURL) {
        this.sendIMURL = sendIMURL;
    }

    public void setCreateIMChatSessionURL(String createIMChatSessionURL) {
        this.createIMChatSessionURL = createIMChatSessionURL;
    }

    public void setChatRequestDataConfirmed(String chatRequestDataConfirmed) {
        this.chatRequestDataConfirmed = chatRequestDataConfirmed;
    }

    public void setChatSubscriptionURL(String chatSubscriptionURL) {
        this.chatSubscriptionURL = chatSubscriptionURL;
    }

    public void setNotificationChannelURL(String notificationChannelURL) {
        this.notificationChannelURL = notificationChannelURL;
    }

    public void setValidLongPoll(String validLongPoll) {
        this.validLongPoll = validLongPoll;
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

    public void setAddressBookSubscriptionURL(String addressBookSubscriptionURL) {
        this.addressBookSubscriptionURL = addressBookSubscriptionURL;
    }

    public void setAddressBookRequestData(String addressBookRequestData) {
        this.addressBookRequestData = addressBookRequestData;
    }

}
