package com.aepona.rcs.test.chat;

import java.io.IOException;
import java.util.Properties;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
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
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/application-context.xml" })
public class ConfirmedChatTest {

    private final Logger LOGGER = LoggerFactory.getLogger(ConfirmedChatTest.class);

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

    private String sendMessageStatusURL;

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
        TestUtils.subscribeToChatNotifications(testUser, chatSubscriptionURL, chatRequestDataConfirmed, apiVersion, applicationUsername, applicationPassword);
        TestUtils.clearPendingNotifications(testUser, applicationUsername, applicationPassword);
        LOGGER.info("User 1 has been Initalised!");
    }

    @Test
    public void createChatSession() throws JsonGenerationException, JsonMappingException, IOException {
        String test = "Creating Chat Session...User1 (Sender) User2 (Reciever)";
        startTest(test);
        // Variables...
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String encodedSenderAddress = encode(sender);
        String encodedReceiverAddress = encode(receiver);
        String endpoint = createIMChatSessionURL;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String requestData;
        String url = replaceLong(endpoint, apiVersion, sender, receiver);
        // Build Request Body...
        ChatSessionInformation chatSessionInformation =
                new ChatSessionInformation(subject, senderSIP, senderUsername, receiver, receiverUsername);
        ObjectMapper mapper = new ObjectMapper();
        requestData = "{\"chatSessionInformation\":" + mapper.writeValueAsString(chatSessionInformation) + "}";
        // HTTP POST Request to Create IM Session...
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
                           .body("chatSessionInformation.status",
                                 Matchers.equalTo("Invited"),
                                 "chatSessionInformation.originatorAddress",
                                 Matchers.equalTo(senderSIP),
                                 "chatSessionInformation.tParticipantAddress",
                                 Matchers.equalTo(receiver),
                                 "chatSessionInformation.resourceURL",
                                 StringContains.containsString(encodedSenderAddress + "/oneToOne/"
                                         + encodedReceiverAddress))
                           .post(url);

        // Log out Details...
        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Chat Session URL = " + jsonData.getString("chatSessionInformation.resourceURL"));

        endTest(test);
    }

    @Test
    public void checkIMNotificationsUser2() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        String test = "Checking IM Notifications for User 2 (" + user2 + ")";
        startTest(test);
        String userID = user2;
        String cleanSenderSIP = cleanPrefix(senderSIP);
        String url = userTwo.getChannelURL();

        Response notificationResponse =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.chatSessionInvitationNotification", Matchers.notNullValue(),
                                 "notificationList.messageNotification.dateTime", Matchers.notNullValue(),
                                 "notificationList.messageNotification.messageId", Matchers.notNullValue(),
                                 "notificationList.messageNotification.senderAddress[0]",
                                 Matchers.containsString(cleanSenderSIP))
                           .post(url);

        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        LOGGER.info("Body = " + notificationResponse.asString());

        JsonPath jsonData = notificationResponse.jsonPath();
        receiveSessionURL = jsonData.getString("notificationList.chatSessionInvitationNotification[0].link[0].href");
        LOGGER.info("Extracted receiveSessionURL=" + receiveSessionURL);
        receiveSessionID = jsonData.getString("notificationList.messageNotification.sessionId[0]");
        LOGGER.info("Extracted receiveSessionID=" + receiveSessionID);
        receiveMessageID = jsonData.getString("notificationList.messageNotification.messageId[0]");
        LOGGER.info("Extracted receiveMessageID=" + receiveMessageID);
        endTest(test);
    }

    @Test
    public void acceptChatSession() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        // Create Chat Session and Check Notifications for Receiving contact
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);

        String test = "Receiving contact " + receiver + " accepting the chat session with user " + sender;
        startTest(test);

        String contactID = senderSIP;
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

    @Test
    public void declineChatSession() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        // Create Chat Session and Check Notifications for Receiving contact
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);

        String test = "Receiving contact " + receiver + " declining the chat session with user " + sender;
        startTest(test);

        String contactID = senderSIP;
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
        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void declinedChatSessionNotification() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        // Create Chat Session and Check Notifications for Receiving contact
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);

        String test = "Receiving contact " + receiver + " declining the chat session with user " + sender;
        startTest(test);

        String contactID = senderSIP;
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
        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
        sleep();
        test = "Checking User 1 Notifications for Declined Session Notification";
        startTest(test);

        url = userOne.getChannelURL();
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

        /*
         * {"notificationList": [{"messageStatusNotification":
         * {"callbackData":"GSMA1", "link": [{"rel":"ChatMessage", "href":
         * "http://api.oneapi-gw.gsma.com/chat/0.1/%2B441110000001/oneToOne/sip%3A%2B441110000002%40rcstestconnect.net//messages/1355246102378-1656522991"
         * }], "status":"Delivered", "messageId":"1355246102378-1656522991"}},
         * {"chatEventNotification": {"callbackData":"GSMA1", "link":
         * [{"rel":"ChatSessionInformation", "href":
         * "http://api.oneapi-gw.gsma.com/chat/0.1/%2B441110000001/oneToOne/sip%3A%2B441110000002%40rcstestconnect.net/247855723"
         * }], "eventType":"Declined", "sessionId":"247855723"}}]}
         */

        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        // LOGGER.info("Body = "+notificationResponse.asString());
        LOGGER.info("Notification Event Type : "
                + notificationResponse.jsonPath().getString("notificationList[1].chatEventNotification.eventType"));
        endTest(test);

    }

    @Test
    public void checkNotificationsUser1() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        // Create Chat Session and Check Notifications for Receiving contact
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);

        String test = "Checking Notifications for User (" + sender + ")";
        startTest(test);
        String url = userOne.getChannelURL();
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

    @Test
    public void sendIMChat() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        // Create Chat Session and Check Notifications for Receiving contact
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);

        String test = "Sending an IM Chat from User (" + sender + ") to Contact (" + receiver + ")";
        startTest(test);

        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        ChatMessage chatMessage = new ChatMessage(text, reportRequest);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"chatMessage\":" + mapper.writeValueAsString(chatMessage) + "}";
        userID = user1;
        contactID = contact2;
        String encodedSender = encode(sender);
        String encodedContact = encode(receiver);
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, sessionID);

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
                           .body("resourceReference.resourceURL",
                                 StringContains.containsString(encodedSender + "/oneToOne/" + encodedContact + "/"
                                         + sessionID + "/messages/"))
                           .post(url);

        sendMessageStatusURL = response.jsonPath().getString("resourceReference.resourceURL");
        String[] parts = sendMessageStatusURL.split("/messages/");
        sentMessageID = parts[1].replaceAll("/status", "");
        LOGGER.info("Response = " + response.getStatusCode() + " / " + response.asString());
        LOGGER.info("resourceURL = " + sendMessageStatusURL);
        LOGGER.info("sentMessageID = " + sentMessageID);
        endTest(test);
    }

    @Test
    public void checkDeliveryUser2Notifications() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);
        sendIMChat(sender, receiver, text, reportRequest, sessionID);
        sleep();

        String test = "Check User/Contact (" + receiver + ") Notifications for Delivery";
        startTest(test);
        String cleanSenderSIP = cleanPrefix(senderSIP);
        String url = userTwo.getChannelURL();
        Response notificationResponse =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.messageNotification[0].senderAddress",
                                 Matchers.containsString(cleanSenderSIP),
                                 "notificationList.messageNotification[0].chatMessage.text", Matchers.equalTo(text))
                           .post(url);
        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        LOGGER.info("Body = " + notificationResponse.asString());
        LOGGER.info("Received IM = "
                + notificationResponse.jsonPath().getString("notificationList[0].messageNotification.chatMessage.text"));
        endTest(test);
    }

    /**
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    @Test
    public void checkingIMNotificationsForUser1() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);
        sendIMChat(sender, receiver, text, reportRequest, sessionID);
        sleep();
        userID = user1;
        String test = "Checking IM Notifications for User (" + userID + ") to get Send Status";
        startTest(test);
        String url = userOne.getChannelURL();

        Response notificationResponse =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.messageStatusNotification", Matchers.notNullValue())
                           .post(url);

        /*
         * Body = {"notificationList":[{"messageStatusNotification":
         * {"callbackData":"GSMA1","link": [{ "rel":"ChatMessage", "href":
         * "http://api.oneapi-gw.gsma.com/chat/0.1/%2B441110000001/oneToOne/sip%3A%2B441110000002%40rcstestconnect.net/1444626309/messages/1355225671110--104466481"
         * }] ,"status":"Delivered","messageId":"1355225671110--104466481"}}]}
         */

        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        LOGGER.info("Body = " + notificationResponse.asString());
        LOGGER.info("Status = "
                + notificationResponse.jsonPath().getString("notificationList[0].messageStatusNotification.status"));
        endTest(test);
    }

    @Test
    public void sendingReplyIM() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);
        sendIMChat(sender, receiver, text, reportRequest, sessionID);
        sleep();
        userID = user1;
        int i = 1;
        checkSendStatus(userOne);
        sleep();

        // SENDING REPLY
        String test = "Sending Reply....";
        startTest(test);
        sender = user2;
        senderSIP = contact2;
        receiver = contact1;
        text = "Hello, Test Reply to User 1";
        sendIMChat(sender, receiver, text, reportRequest, receiveSessionID);
        endTest(test);
    }

    @Test
    public void checkReplyDelivery() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);
        sendIMChat(sender, receiver, text, reportRequest, sessionID);
        sleep();
        userID = user1;
        int i = 1;
        checkSendStatus(userOne);
        sleep();
        sender = user2;
        senderSIP = contact2;
        receiver = contact1;
        text = "Hello, Test Reply to User 1";
        sendIMChat(sender, receiver, text, reportRequest, receiveSessionID);
        sleep();
        // Checking User1 Notifications for Incoming IM Message
        checkDelivery(userOne);

    }

    @Test
    public void checkReplyStatus() throws JsonGenerationException, JsonMappingException, IOException {
        String sender = user1;
        String senderSIP = contact1;
        String receiver = contact2;
        String subject = "Test Created IM Session";
        String senderUsername = "MO-HOST";
        String receiverUsername = "MT-RECEIVER";
        String userID = user2;
        String contactID = contact1;
        String text = "Hello, Test Message to User 2";
        String reportRequest = "Displayed";
        createChatSession(sender, receiver, senderSIP, subject, senderUsername, receiverUsername);
        sleep();
        checkIMNotifications(userID);
        acceptSession(userID, contactID);
        sleep();
        checkSessionStatusNotification(sender);
        sendIMChat(sender, receiver, text, reportRequest, sessionID);
        sleep();
        userID = user1;
        int i = 1;
        checkSendStatus(userOne);
        sleep();
        sender = user2;
        senderSIP = contact2;
        receiver = contact1;
        text = "Hello, Test Reply to User 1";
        sendIMChat(sender, receiver, text, reportRequest, receiveSessionID);
        sleep();
        checkDelivery(userOne);
        sleep();
        // Checking Delivery Status
        userID = user2;
        i = 2;
        String test = "Checking IM Notifications for User (" + userID + ") to get Send Status";
        startTest(test);
        String url = userTwo.getChannelURL();

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

        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        LOGGER.info("Status = "
                + notificationResponse.jsonPath().getString("notificationList[1].messageStatusNotification.status"));
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
        sendMessageStatusURL = null;
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
        LOGGER.info("Chat session URL = " + jsonData.getString("chatSessionInformation.resourceURL"));
        LOGGER.info("Received Response = " + response.getStatusCode());
        endTest(test);
    }

    private void checkIMNotifications(String userID) {
        String test = "Checking IM Notifications for User (" + userID + ")";
        startTest(test);
        String url = userTwo.getChannelURL();

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

    private void checkSessionStatusNotification(String sender) {
        String test = "Checking Notifications for User (" + sender + ")";
        startTest(test);
        String url = userOne.getChannelURL();
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

    private void sendIMChat(String sender, String receiver, String text, String reportRequest, String session)
            throws JsonGenerationException, JsonMappingException, IOException {
        String test = "Sending an IM Chat from User (" + sender + ") to Contact (" + receiver + ")";
        startTest(test);
        ChatMessage chatMessage = new ChatMessage(text, reportRequest);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"chatMessage\":" + mapper.writeValueAsString(chatMessage) + "}";
        String userID = sender;
        String contactID = receiver;
        String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, session);

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

        sendMessageStatusURL = response.jsonPath().getString("resourceReference.resourceURL");
        String[] parts = sendMessageStatusURL.split("/messages/");
        sentMessageID = parts[1].replaceAll("/status", "");
        LOGGER.info("Response = " + response.getStatusCode() + " / " + response.asString());
        LOGGER.info("resourceURL = " + sendMessageStatusURL);
        LOGGER.info("sentMessageID = " + sentMessageID);
        endTest(test);
    }

    private void checkDelivery(final TestSubscriber testSubscriber) {
        String test = "Check User/Contact (" + testSubscriber + ") Notifications for Delivery";
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
        LOGGER.info("Received IM = "
                + notificationResponse.jsonPath().getString("notificationList[0].messageNotification.chatMessage.text"));
        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        endTest(test);
    }

    private void checkSendStatus(final TestSubscriber testSubscriber) {
        String test = "Checking IM Notifications for User (" + testSubscriber + ") to get Send Status";
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

        LOGGER.info("Received Response = " + notificationResponse.getStatusCode());
        // LOGGER.info("Received Response = "
        // + notificationResponse.asString());
        LOGGER.info("Status = "
                + notificationResponse.jsonPath().getString("notificationList[0].messageStatusNotification.status"));
        endTest(test);
    }

    // ********* GENERAL METHODS **********
    private void sleep() {
        try {
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
