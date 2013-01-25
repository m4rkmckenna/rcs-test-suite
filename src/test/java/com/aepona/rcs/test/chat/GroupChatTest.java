package com.aepona.rcs.test.chat;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.Arrays;

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
public class GroupChatTest {

    private static final String SESSION_ID = "sessionKey";

    private static final String SESSION_URL = "sessionUrl";

    private final Logger LOGGER = LoggerFactory.getLogger(GroupChatTest.class);

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

    @Value("${user4}")
    protected String user4;

    @Value("${contact1}")
    protected String contact1;

    @Value("${contact2}")
    protected String contact2;

    @Value("${contact3}")
    protected String contact3;

    @Value("${contact4}")
    protected String contact4;

    @Value("${chatRequestDataConfirmed}")
    protected String chatRequestDataConfirmed;

    @Value("${chatRequestDataAdhoc}")
    protected String chatRequestDataAdhoc;

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

    @Value("${groupChatURL}")
    protected String groupChatURL;

    @Value("${groupChatSessionURL}")
    protected String groupChatSessionURL;

    @Value("${groupChatMessageURL}")
    protected String groupChatMessageURL;

    @Value("${groupChatParticipantsURL}")
    protected String groupChatParticipantsURL;

    @Value("${groupChatParticipantURL}")
    protected String groupChatParticipantURL;

    @Value("${groupChatParticipantStatusURL}")
    protected String groupChatParticipantStatusURL;

    String lastTest = null;

    Boolean initialised = false;

    private TestSubscriber userOne;

    private TestSubscriber userTwo;

    private TestSubscriber userThree;

    private TestSubscriber userFour;

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
        userThree = new TestSubscriber();
        userThree.setUserID(user3);
        userFour = new TestSubscriber();
        userFour.setUserID(user4);
        initialiseUser(userOne);
        initialiseUser(userTwo);
        initialiseUser(userThree);
        initialiseUser(userFour);
    }

    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(userOne, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userTwo, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userThree, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userFour, applicationUsername, applicationPassword);
    }

    public void initialiseUser(final TestSubscriber testUser) {
        LOGGER.info("Initialising User " + testUser);
        TestUtils.registerUser(testUser, registerURL, apiVersion, applicationUsername, applicationPassword);
        TestUtils.startNotificationChannel(testUser, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToChatNotifications(testUser, chatSubscriptionURL, chatRequestDataConfirmed, apiVersion,
                                               applicationUsername, applicationPassword);
        TestUtils.clearPendingNotifications(testUser, applicationUsername, applicationPassword);
        LOGGER.info("User has been Initalised as " + testUser);
    }

    @Test
    public void groupChatSession() throws JsonGenerationException, JsonMappingException, IOException {
        ParticipantInformation originator =
                new ParticipantInformation(contact1, "Rhiannon", true, UUID.randomUUID().toString());
        LOGGER.info("Participant Information (originator) = " + originator.toString());
        ParticipantInformation participant1 =
                new ParticipantInformation(contact2, "Mark", false, UUID.randomUUID().toString());
        LOGGER.info("Participant Information (contact) = " + participant1.toString());
        ParticipantInformation participant2 =
                new ParticipantInformation(contact3, "Judith", false, UUID.randomUUID().toString());
        LOGGER.info("Participant Information (contact) = " + participant2.toString());
        
        GroupChatSessionInformation sessionInformation =
                new GroupChatSessionInformation("Subject: Trial", new ParticipantInformation[]{originator,
                                                                                               participant1,
                                                                                               participant2},
                                                UUID.randomUUID().toString());
        LOGGER.info("Session information = " + sessionInformation.toString());
        String test =
                "Creating Group Chat between User 1 (" + contact1 + "), User 2 (" + contact2 + ") and User 3 ("
                        + contact3 + ")";
        startTest(test);

        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"groupChatSessionInformation\":" + mapper.writeValueAsString(sessionInformation) + "}";
        String userID = user1;
        String cleanUserID = cleanPrefix(userID);
        String url = replace(groupChatURL, apiVersion, userID);
        LOGGER.info("URL = " + baseURI + url);
        LOGGER.info("Request Body = " + requestData);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void checkNotificationsForUser1() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        String test = "Check IM Notifications for User 1 (" + userID + ")";
        startTest(test);

        String url = userOne.getChannelURL();
        LOGGER.info("URL = " + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("notificationList.chatEventNotification[0].eventType", Matchers.equalTo("Successful"),
                                 "notificationList.chatEventNotification[0].link.rel[0]",
                                 Matchers.equalTo("GroupChatSessionInformation"),
                                 "notificationList.chatEventNotification[0].sessionId", Matchers.notNullValue(),
                                 "notificationList.participantStatusNotification", Matchers.notNullValue())
                           .post(url);
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Session ID = " + jsonData.getString("notificationList.chatEventNotification[0].sessionId"));
        LOGGER.info("Session URL = " + jsonData.getString("notificationList[0].chatEventNotification.link.href[0]"));
        endTest(test);
    }

    @Test
    public void checkNotificationsForUser2() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user2;
        int i = 2;
        createGroupChat();
        sleep();

        String test = "Check IM Notifications for User (" + userID + ")";
        startTest(test);

        String url = userTwo.getChannelURL();
        LOGGER.info("URL = " + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Session ID = "
                + jsonData.getString("notificationList.groupSessionInvitationNotification.sessionId"));
        LOGGER.info("Session URL = "
                + jsonData.getString("notificationList[0].groupSessionInvitationNotification.link[0].href"));
        endTest(test);
    }

    @Test
    public void checkNotificationsForUser3() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user3;
        int i = 3;
        createGroupChat();
        sleep();

        String test = "Check IM Notifications for User (" + userID + ")";
        startTest(test);

        String url = userThree.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Session ID = "
                + jsonData.getString("notificationList.groupSessionInvitationNotification.sessionId"));
        LOGGER.info("Session URL = "
                + jsonData.getString("notificationList[0].groupSessionInvitationNotification.link[0].href"));
        endTest(test);
    }

    @Test
    public void getSessionInformation1() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        sleep();
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);

        String test = "Getting Session Information for User (" + userID + ")";
        startTest(test);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID);
        String cleanUserID = cleanPrefix(userID);
        String url = replaceLong(groupChatSessionURL, apiVersion, userID, sessionID);
        LOGGER.info("URL = " + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("groupChatSessionInformation.resourceURL",
                                 StringContains.containsString(cleanUserID + "/group/" + sessionID),
                                 "groupChatSessionInformation.subject", Matchers.equalTo("Subject: Trial"))
                           .get(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void getSessionInformation2() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        sleep();
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);

        userID = user2;
        i = 2;
        getSessionInformation(userTwo);
    }

    @Test
    public void getSessionInformation3() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        sleep();
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);

        userID = user3;
        i = 3;
        getSessionInformation(userThree);
    }

    @Test
    public void getParticipantsInformationPriorAcceptance() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        sleep();
        String test = "Getting ALL participants information for User " + i + " (" + userID + ")";
        startTest(test);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);
        LOGGER.info("URL = " + baseURI + url);
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("participantList", Matchers.notNullValue(), "participantList.participant",
                                 Matchers.notNullValue())
                           .get(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Status for Paticipant 1 = "
                + response.jsonPath().getString("participantList.participant[0].status"));
        LOGGER.info("Status for Paticipant 2 = "
                + response.jsonPath().getString("participantList.participant[1].status"));
        endTest(test);
    }

    @Test
    public void sendingAccept1() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);

        String test = "User 2 Accepting Group Chat Request";
        startTest(test);
        userID = user2;
        i = 2;
        String jsonRequestData = "{\"participantSessionStatus\":{\"status\":\"Connected\"}}";
        String sessionID = userTwo.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String participantID = contact2;
        String url = replaceExtraLong(groupChatParticipantStatusURL, apiVersion, userID, sessionID, participantID);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(jsonRequestData)
                           .expect()
                           .log()
                           .ifError()
                           ./* statusCode(204). */
                           put(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void getParticipantsInformationAfterAcceptDecline() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        sleep();

        String test = "User 3 Declining Group Chat Invitation";
        startTest(test);

        userID = user3;
        i = 3;
        String participantID = contact3;
        String sessionID = userThree.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceExtraLong(groupChatParticipantURL, apiVersion, userID, sessionID, participantID);
        LOGGER.info("URL = " + baseURI + url);
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

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

        sleep();
        sleep();

        userID = user1;
        i = 1;

        test = "Getting ALL participants information for User " + i + " (" + userID + ")";
        startTest(test);
        sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);
        LOGGER.info("URL = " + baseURI + url);
        response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .get(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void getParticipantsInformationAfterAcceptance() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        getParticipantsInformation(userOne);
    }

    @Test
    public void participantGettingParticipantsInformation1() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        getParticipantsInformation(userTwo);
    }

    @Test
    public void participantGettingParticipantsInformation2() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        getParticipantsInformation(userThree);
    }

    @Test
    public void sendMessageFromOriginator() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        String test = "Sending Message from Originator to Participants";
        startTest(test);
        String text = "Hello Everybody!";
        String reportRequest = "Displayed";
        String encodedUserID = encode(userID);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatMessageURL, apiVersion, userID, sessionID);
        LOGGER.info("URL = " + baseURI + url);
        ChatMessage chatMessage = new ChatMessage(text, reportRequest);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"chatMessage\":" + mapper.writeValueAsString(chatMessage) + "}";

        LOGGER.info("Request Body = " + requestData);

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
                                 StringContains.containsString(encodedUserID + "/group/"))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void checkNotificationsForMessageSent() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        String text = "Hello Everybody!";
        sendMessage(userOne, text);

        checkIMNotificationsOriginator(userOne);
    }

    @Test
    public void checkNotificationsForMessageArrival1() throws JsonGenerationException, JsonMappingException,
            IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        String text = "Hello Everybody!";
        sendMessage(userOne, text);
        String originatorSIP = contact1;
        sleep();
        checkIMNotificationsParticipant(userTwo, originatorSIP, text);
        checkIMNotificationsParticipant(userThree, originatorSIP, text);
    }

    @Test
    public void sendMessageFromUser2() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        String text = "Hello Everybody!";
        sendMessage(userOne, text);
        String originatorSIP = contact1;
        sleep();
        checkIMNotificationsParticipant(userTwo, originatorSIP, text);
        checkIMNotificationsParticipant(userThree, originatorSIP, text);

        userID = user2;
        i = 2;
        text = "Hello! User 2 Here...";
        sendMessage(userTwo, text);
    }

    @Test
    public void checkNotificationOfMessageArrival2() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);
        sleep();
        sleep();
        sleep();

        String text = "Hello Everybody!";
        sendMessage(userOne, text);
        String originatorSIP = contact1;
        sleep();
        sleep();
        checkIMNotificationsParticipant(userTwo, originatorSIP, text);
        checkIMNotificationsParticipant(userThree, originatorSIP, text);

        userID = user2;
        i = 2;
        text = "Hello! User 2 Here...";
        sendMessage(userTwo, text);
        sleep();
        sleep();

        checkIMNotificationsParticipant(userOne, contact2, text);
        checkIMNotificationsParticipant(userThree, contact2, text);
    }

    @Test
    public void addUserToChat() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);

        ParticipantInformation participant3 =
                new ParticipantInformation(contact4, "Ronan", false, UUID.randomUUID().toString());

        String test = "Adding User 4 to the Group Chat";
        startTest(test);

        ParticipantInformation[] participants = new ParticipantInformation[]{participant3};
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"participantList\":{\"participant\":" + mapper.writeValueAsString(participants) + "}}";
        String cleanUserID = cleanPrefix(userID);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);
        LOGGER.info("URL = " + baseURI + url);
        LOGGER.info("Request Data = " + requestData);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void newUserAccept() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);

        ParticipantInformation participant3 =
                new ParticipantInformation(contact4, "Ronan", false, UUID.randomUUID().toString());

        String test = "Adding User 4 to the Group Chat";
        startTest(test);

        ParticipantInformation[] participants = new ParticipantInformation[]{participant3};
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"participantList\":{\"participant\":" + mapper.writeValueAsString(participants) + "}}";
        String cleanUserID = cleanPrefix(userID);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

        sleep();
        checkNotificationsParticipant(userFour);
        sleep();
        acceptInvitation(userFour, contact4);
        sleep();
        sleep();
        getParticipantsInformationNew(userOne);
    }

    @Test
    public void sendMessageFromNewParticipant() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);

        ParticipantInformation participant3 =
                new ParticipantInformation(contact4, "Ronan", false, UUID.randomUUID().toString());

        String test = "Adding User 4 to the Group Chat";
        startTest(test);

        ParticipantInformation[] participants = new ParticipantInformation[]{participant3};
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"participantList\":{\"participant\":" + mapper.writeValueAsString(participants) + "}}";
        String cleanUserID = cleanPrefix(userID);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

        sleep();
        checkNotificationsParticipant(userFour);
        sleep();
        acceptInvitation(userFour, contact4);

        test = "Sending Message from New User (user 4)";
        startTest(test);
        userID = user4;
        i = 4;
        String text = "Hello! Thanks for including me!!";
        sendMessage(userFour, text);
        endTest(test);
    }

    @Test
    public void leaveChat() throws JsonGenerationException, JsonMappingException, IOException {
        String userID = user1;
        int i = 1;
        createGroupChat();
        sleep();
        checkNotificationsOriginator(userOne);
        checkNotificationsParticipant(userTwo);
        checkNotificationsParticipant(userThree);
        acceptInvitation(userTwo, contact2);
        acceptInvitation(userThree, contact3);

        ParticipantInformation participant3 =
                new ParticipantInformation(contact4, "Ronan", false, UUID.randomUUID().toString());

        String test = "Adding User 4 to the Group Chat";
        startTest(test);

        ParticipantInformation[] participants = new ParticipantInformation[]{participant3};
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"participantList\":{\"participant\":" + mapper.writeValueAsString(participants) + "}}";
        String cleanUserID = cleanPrefix(userID);
        String sessionID = userOne.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, userID, sessionID);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);

        sleep();
        checkNotificationsParticipant(userFour);
        sleep();
        acceptInvitation(userFour, contact4);

        test = "User 3 leaves chat";
        startTest(test);
        userID = user3;
        i = 3;
        sessionID = userThree.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String participantID = contact3;
        url = replaceExtraLong(groupChatParticipantURL, apiVersion, userID, sessionID, participantID);
        LOGGER.info("URL = " + baseURI + url);
        response =
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

    // ************************* HELPERS *******************************
    private void createGroupChat() throws JsonGenerationException, JsonMappingException, IOException {
        ParticipantInformation originator =
                new ParticipantInformation(contact1, "Rhiannon", true, UUID.randomUUID().toString());
        ParticipantInformation participant1 =
                new ParticipantInformation(contact2, "Mark", false, UUID.randomUUID().toString());
        ParticipantInformation participant2 =
                new ParticipantInformation(contact3, "Judith", false, UUID.randomUUID().toString());

        GroupChatSessionInformation sessionInformation =
                new GroupChatSessionInformation("Subject: Trial", new ParticipantInformation[]{originator,
                                                                                               participant1,
                                                                                               participant2},
                                                UUID.randomUUID().toString());

        String test =
                "Creating Group Chat between User 1 (" + contact1 + "), User 2 (" + contact2 + ") and User 3 ("
                        + contact3 + ")";
        startTest(test);

        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"groupChatSessionInformation\":" + mapper.writeValueAsString(sessionInformation) + "}";

        String userID = user1;
        String cleanUserID = cleanPrefix(userID);
        String url = replace(groupChatURL, apiVersion, userID);
        LOGGER.info("URL = " + baseURI + url);
        LOGGER.info("Request Body = " + requestData);

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
                           .body("resourceReference.resourceURL", StringContains.containsString(cleanUserID))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    private void checkNotificationsOriginator(final TestSubscriber testUser) {
        String test = "Check IM Notifications for User (" + testUser + ")";
        startTest(test);

        String url = testUser.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response Received = " + response.getStatusCode());
        testUser.getAdditionalProperties().put(SESSION_ID, jsonData.getString("notificationList.chatEventNotification[0].sessionId"));
        testUser.getAdditionalProperties().put(SESSION_URL, jsonData.getString("notificationList[0].chatEventNotification.link.href[0]"));
        LOGGER.info("Session ID = " + testUser.getAdditionalProperties().get(SESSION_ID));
        LOGGER.info("Session URL = " + testUser.getAdditionalProperties().get(SESSION_URL));
        endTest(test);
    }

    public void checkNotificationsParticipant(final TestSubscriber testUser) {
        String test = "Check IM Notifications for User (" + testUser + ")";
        startTest(test);

        String url = testUser.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response Received = " + response.getStatusCode());
        testUser.getAdditionalProperties().put(SESSION_ID, jsonData.getString("notificationList.groupSessionInvitationNotification.sessionId"));
        testUser.getAdditionalProperties().put(SESSION_URL, jsonData.getString("notificationList[0].groupSessionInvitationNotification.link[0].href"));
        LOGGER.info("Session ID = " + testUser.getAdditionalProperties().get(SESSION_ID));
        LOGGER.info("Session URL = " + testUser.getAdditionalProperties().get(SESSION_URL));
        endTest(test);
    }

    private void getSessionInformation(final TestSubscriber testSubscriber) {
        String test = "Getting Session Information for User (" + testSubscriber + ")";
        startTest(test);
        String sessionID = testSubscriber.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String cleanUserID = cleanPrefix(testSubscriber.getUserID());
        String url = replaceLong(groupChatSessionURL, apiVersion, testSubscriber.getUserID(), sessionID);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("groupChatSessionInformation.resourceURL",
                                 StringContains.containsString(cleanUserID + "/group/" + sessionID),
                                 "groupChatSessionInformation.subject", Matchers.equalTo("Subject: Trial"))
                           .get(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    private void acceptInvitation(final TestSubscriber testSubscriber, String participantID) {
        String test = "Accepting Group Chat Request - User " + testSubscriber;
        startTest(test);
        String jsonRequestData = "{\"participantSessionStatus\":{\"status\":\"Connected\"}}";
        String sessionID = testSubscriber.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceExtraLong(groupChatParticipantStatusURL, apiVersion, testSubscriber.getUserID(), sessionID, participantID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(jsonRequestData)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(204)
                           .put(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        endTest(test);
    }

    private void getParticipantsInformation(final TestSubscriber testSubscriber) {
        String test = "Getting ALL participants information for User (" + testSubscriber + ")";
        startTest(test);
        String sessionID = testSubscriber.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, testSubscriber.getUserID(), sessionID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("participantList", Matchers.notNullValue(), "participantList.participant",
                                 Matchers.notNullValue())
                           .get(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Status for Paticipant " + response.jsonPath().getString("participantList.participant[0].address")
                + " = " + response.jsonPath().getString("participantList.participant[0].status"));
        LOGGER.info("Status for Paticipant " + response.jsonPath().getString("participantList.participant[1].address")
                + " = " + response.jsonPath().getString("participantList.participant[1].status"));
        endTest(test);
    }

    private void getParticipantsInformationNew(final TestSubscriber testSubscriber) {
        String test = "Getting ALL participants information for User " + testSubscriber;
        startTest(test);
        String sessionID = testSubscriber.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatParticipantsURL, apiVersion, testSubscriber.getUserID(), sessionID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("participantList", Matchers.notNullValue(), "participantList.participant",
                                 Matchers.notNullValue())
                           .get(url);

        LOGGER.info("Response Recieved = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Status for Paticipant " + response.jsonPath().getString("participantList.participant[0].address")
                + " = " + response.jsonPath().getString("participantList.participant[0].status"));
        LOGGER.info("Status for Paticipant " + response.jsonPath().getString("participantList.participant[1].address")
                + " = " + response.jsonPath().getString("participantList.participant[1].status"));
        LOGGER.info("Status for Paticipant " + response.jsonPath().getString("participantList.participant[2].address")
                + " = " + response.jsonPath().getString("participantList.participant[2].status"));
        endTest(test);
    }

    private void sendMessage(final TestSubscriber testSubscriber, String text) throws JsonGenerationException, JsonMappingException,
            IOException {
        String test = "Sending Message";
        startTest(test);
        String reportRequest = "Displayed";
        String encodedUserID = encode(testSubscriber.getUserID());
        String sessionID = testSubscriber.getAdditionalProperties().get(SESSION_ID).replace("[", "").replace("]", "");
        String url = replaceLong(groupChatMessageURL, apiVersion, testSubscriber.getUserID(), sessionID);

        ChatMessage chatMessage = new ChatMessage(text, reportRequest);
        ObjectMapper mapper = new ObjectMapper();
        String requestData = "{\"chatMessage\":" + mapper.writeValueAsString(chatMessage) + "}";

        LOGGER.info("Request Body = " + requestData);

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
                                 StringContains.containsString(encodedUserID + "/group/"))
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    private void checkIMNotificationsOriginator(final TestSubscriber testSubscriber) {
        String test = "Check IM Notifications for User (" + testSubscriber + ")";
        startTest(test);

        String url = testSubscriber.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    private void checkIMNotificationsParticipant(final TestSubscriber testSubscriber, String originatorSIP, String text) {
        String test = "Check IM Notifications for User (" + testSubscriber + ")";
        startTest(test);

        String url = testSubscriber.getChannelURL();
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    // METHODS AND RESOURCES


    // ********* GENERAL METHODS **********
    public void start() {
        if (!initialised) {
            RestAssured.baseURI = baseURI;
            RestAssured.port = port;
            RestAssured.basePath = "";
            RestAssured.urlEncodingEnabled = true;
            initialised = true;
        }
    }

    private void sleep() {
        try {
            LOGGER.info("Waiting........");
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

    private String replace(String url, String apiVersion, String userID) {
        return url.replace("{apiVersion}", apiVersion).replace("{userID}", userID);
    }

    private String replaceLong(String url, String apiVersion, String userID, String sessionID) {
        return url.replace("{apiVersion}", apiVersion).replace("{userID}", userID).replace("{sessionID}", sessionID);
    }

    private String replaceExtraLong(String url, String apiVersion, String userID, String sessionID, String participantID) {
        return url.replace("{apiVersion}", apiVersion)
                  .replace("{userID}", userID)
                  .replace("{participantID}", participantID)
                  .replace("{sessionID}", sessionID);
    }

    public String encode(String userID) {
        return userID.replaceAll("\\:", "%3A").replaceAll("\\+", "%2B").replaceAll("\\@", "%40");
    }

    public String cleanPrefix(String userID) {
        return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "").replaceAll("\\+", "").replaceAll("\\:", "");
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

    public void setUser4(String user4) {
        this.user4 = user4;
    }

    public void setContact1(String contact1) {
        this.contact1 = contact1;
    }

    public void setContact2(String contact2) {
        this.contact2 = contact2;
    }

    public void setContact3(String contact3) {
        this.contact3 = contact3;
    }

    public void setContact4(String contact4) {
        this.contact4 = contact4;
    }

    public void setChatRequestDataConfirmed(String chatRequestDataConfirmed) {
        this.chatRequestDataConfirmed = chatRequestDataConfirmed;
    }

    public void setChatRequestDataAdhoc(String chatRequestDataAdhoc) {
        this.chatRequestDataAdhoc = chatRequestDataAdhoc;
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

    public void setGroupChatURL(String groupChatURL) {
        this.groupChatURL = groupChatURL;
    }

    public void setGroupChatSessionURL(String groupChatSessionURL) {
        this.groupChatSessionURL = groupChatSessionURL;
    }

    public void setGroupChatMessageURL(String groupChatMessageURL) {
        this.groupChatMessageURL = groupChatMessageURL;
    }

    public void setGroupChatParticipantsURL(String groupChatParticipantsURL) {
        this.groupChatParticipantsURL = groupChatParticipantsURL;
    }

    public void setGroupChatParticipantURL(String groupChatParticipantURL) {
        this.groupChatParticipantURL = groupChatParticipantURL;
    }

    public void setGroupChatParticipantStatusURL(String groupChatParticipantStatusURL) {
        this.groupChatParticipantStatusURL = groupChatParticipantStatusURL;
    }

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

    static class ParticipantInformation {

        String address;

        String name;

        boolean isOriginator;

        String clientCorrelator;

        String resourceURL;

        public String getAddress() {
            return address;
        }

        public String getName() {
            return name;
        }

        public boolean getIsOriginator() {
            return isOriginator;
        }

        public String getClientCorrelator() {
            return clientCorrelator;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setIsOriginator(boolean isOriginator) {
            this.isOriginator = isOriginator;
        }

        public void setClientCorrelator(String clientCorrelator) {
            this.clientCorrelator = clientCorrelator;
        }

        @JsonIgnore
        public String getResourceURL() {
            return resourceURL;
        }

        public void setResourceURL(String resourceURL) {
            this.resourceURL = resourceURL;
        }

        @Override
		public String toString() {
			return "ParticipantInformation [address=" + address + ", name="
					+ name + ", isOriginator=" + isOriginator
					+ ", clientCorrelator=" + clientCorrelator
					+ ", resourceURL=" + resourceURL + "]";
		}

		public ParticipantInformation(String address, String name, boolean isOriginator, String clientCorrelator) {
            this.address = address;
            this.name = name;
            this.isOriginator = isOriginator;
            this.clientCorrelator = clientCorrelator;
        }
    }

    static class GroupChatSessionInformation {

        String subject;

        ParticipantInformation[] participant;

        String clientCorrelator;

        String resourceURL;

        public String getSubject() {
            return subject;
        }

        public ParticipantInformation[] getParticipant() {
            return participant;
        }

        public String getClientCorrelator() {
            return clientCorrelator;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void setParticipant(ParticipantInformation[] participant) {
            this.participant = participant;
        }

        public void setClientCorrelator(String clientCorrelator) {
            this.clientCorrelator = clientCorrelator;
        }

        @JsonIgnore
        public String getResourceURL() {
            return resourceURL;
        }

        public void setResourceURL(String resourceURL) {
            this.resourceURL = resourceURL;
        }

        @Override
		public String toString() {
			return "GroupChatSessionInformation [subject=" + subject
					+ ", participant=" + Arrays.toString(participant)
					+ ", clientCorrelator=" + clientCorrelator
					+ ", resourceURL=" + resourceURL + "]";
		}

		public GroupChatSessionInformation(String subject, ParticipantInformation[] participant, String clientCorrelator) {
            this.subject = subject;
            this.participant = participant;
            this.clientCorrelator = clientCorrelator;
        }

    }
}
