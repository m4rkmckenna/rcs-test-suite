package com.aepona.rcs.test.chat;

import java.util.Properties;

import org.hamcrest.core.IsEqual;
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
public class SubscribeTest {

    private final Logger LOGGER = LoggerFactory.getLogger(SubscribeTest.class);

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

    @Value("${invalidUser}")
    protected String invalidUser;

    @Value("${validLongPoll}")
    protected String validLongPoll;

    @Value("${notificationChannelURL}")
    protected String notificationChannelURL;

    @Value("${chatRequestDataAdhoc}")
    protected String chatRequestDataAdhoc;

    @Value("${chatRequestDataConfirmed}")
    protected String chatRequestDataConfirmed;

    @Value("${chatSubscriptionURL}")
    protected String chatSubscriptionURL;

    private Boolean initialised = false;

    private String lastTest = null;

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
    }

    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(userOne, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userTwo, applicationUsername, applicationPassword);
    }

    @Test
    public void subscribeChatNotificationsUser1Adhoc() {
        String userID = user1;
        int i = 1;
        TestUtils.startNotificationChannel(userOne, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);

        String test = "Subscribe User 1 for Chat Notifications (Adhoc)";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userOne.getCallbackURL();
        String requestData = requestDataClean(chatRequestDataAdhoc, clientCorrelator, callback);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

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
                           .body("chatNotificationSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "chatNotificationSubscription.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatNotificationSubscription.callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"), "chatNotificationSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(callback),
                                 "chatNotificationSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"), "chatNotificationSubscription.duration",
                                 IsEqual.equalTo(Integer.valueOf(900)))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Session Subscription URL: " + jsonData.getString("chatNotificationSubscription.resourceURL"));

        endTest(test);

    }

    @Test
    public void subscribeChatNotificationsUser2Adhoc() {
        String userID = user2;
        int i = 2;
        TestUtils.startNotificationChannel(userTwo, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        
        String test = "Subscribe User 2 for Chat Notifications (Adhoc)";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userTwo.getCallbackURL();
        String requestData = requestDataClean(chatRequestDataAdhoc, clientCorrelator, callback);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

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
                           .body("chatNotificationSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "chatNotificationSubscription.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatNotificationSubscription.callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"), "chatNotificationSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(callback),
                                 "chatNotificationSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"), "chatNotificationSubscription.duration",
                                 IsEqual.equalTo(Integer.valueOf(900)))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Session Subscription URL: " + jsonData.getString("chatNotificationSubscription.resourceURL"));

        endTest(test);

    }

    @Test
    public void subscribeChatNotificationsUser1Confirmed() {
        String userID = user1;
        int i = 1 + 2;
        TestUtils.startNotificationChannel(userOne, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);

        String test = "Subscribe User 1 for Chat Notifications (Confirmed)";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userOne.getCallbackURL();
        String requestData = requestDataClean(chatRequestDataConfirmed, clientCorrelator, callback);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

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
                           .body("chatNotificationSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "chatNotificationSubscription.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatNotificationSubscription.callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"), "chatNotificationSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(callback),
                                 "chatNotificationSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"), "chatNotificationSubscription.duration",
                                 IsEqual.equalTo(Integer.valueOf(900)))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Session Subscription URL: " + jsonData.getString("chatNotificationSubscription.resourceURL"));

        endTest(test);

    }

    @Test
    public void subscribeChatNotificationsUser2Confirmed() {
        String userID = user2;
        int i = 2 + 2;
        TestUtils.startNotificationChannel(userTwo, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);

        String test = "Subscribe User 2 for Chat Notifications (Confirmed)";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userTwo.getCallbackURL();
        String requestData = requestDataClean(chatRequestDataConfirmed, clientCorrelator, callback);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

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
                           .body("chatNotificationSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "chatNotificationSubscription.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatNotificationSubscription.callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"), "chatNotificationSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(callback),
                                 "chatNotificationSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"), "chatNotificationSubscription.duration",
                                 IsEqual.equalTo(Integer.valueOf(900)))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Session Subscription URL: " + jsonData.getString("chatNotificationSubscription.resourceURL"));

        endTest(test);

    }

    @Test
    public void subscribeChatNotificationsInvalidUser() {
        String userID = invalidUser;
        int i = 3;
        startInvalidNotificationChannel(userID, i);

        String test = "Subscribe Non-Existant User for Chat Notifications";
        startTest(test);

        LOGGER.error("ERROR: Unable to proceed as Notification Channel has not been created");

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

    private String requestDataClean(String chatRequestData, String clientCorrelator, String callback) {
        String clean = chatRequestData.replace("{CALLBACK}", callback).replace("{CLIENTCORRELATOR}", clientCorrelator);
        return clean;
    }

    public String cleanPrefix(String userID) {
        return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "").replaceAll("\\+", "").replaceAll("\\:", "");
    }

    public void startInvalidNotificationChannel(String userID, int i) {
        String test = "Starting the Notification Channel";
        startTest(test);

        String url = replace(notificationChannelURL, apiVersion, userID);

        // Make HTTP POST Request...
        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(userID, applicationPassword)
                           .body(validLongPoll)
                           .header("accept", "application/json")
                           .header("Content-Type", "application/json")
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(401)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());

        endTest(test);
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

    public void setInvalidUser(String invalidUser) {
        this.invalidUser = invalidUser;
    }

    public void setValidLongPoll(String validLongPoll) {
        this.validLongPoll = validLongPoll;
    }

    public void setNotificationChannelURL(String notificationChannelURL) {
        this.notificationChannelURL = notificationChannelURL;
    }

    public void setChatRequestDataAdhoc(String chatRequestDataAdhoc) {
        this.chatRequestDataAdhoc = chatRequestDataAdhoc;
    }

    public void setChatRequestDataConfirmed(String chatRequestDataConfirmed) {
        this.chatRequestDataConfirmed = chatRequestDataConfirmed;
    }

    public void setChatSubscriptionURL(String chatSubscriptionURL) {
        this.chatSubscriptionURL = chatSubscriptionURL;
    }
}
