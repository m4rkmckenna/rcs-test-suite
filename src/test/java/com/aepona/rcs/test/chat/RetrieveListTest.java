package com.aepona.rcs.test.chat;

import java.util.Properties;

import org.hamcrest.Matchers;
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
public class RetrieveListTest {

    private final Logger LOGGER = LoggerFactory.getLogger(RetrieveIndividualTest.class);

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

    @Value("${urlSplit}")
    protected String urlSplit;

    private Boolean initialised = false;

    private String lastTest = null;

    private TestSubscriber userOne;

    private TestSubscriber userTwo;
    
    private TestSubscriber userThree;

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
    }

    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(userOne, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userTwo, applicationUsername, applicationPassword);
        TestUtils.deleteNotificationChannel(userThree, applicationUsername, applicationPassword);
    }

    @Test
    public void retrieveListChatSubscriptionsUser1() {
        String userID = user1;
        int i = 1;

        TestUtils.startNotificationChannel(userOne, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToChatNotifications(userOne, chatSubscriptionURL, chatRequestDataConfirmed, apiVersion,
                                               applicationUsername, applicationPassword);

        String test = "Retrieve List of all Chat Subscriptions for User 2";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("chatSubscriptionList.chatNotificationSubscription.size()",
                                 Matchers.is(1),
                                 "chatSubscriptionList.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatSubscriptionList.chatNotificationSubscription[0].resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.notifyURL",
                                 IsEqual.equalTo(userOne.getCallbackURL()),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"))
                           .get(url);

        if (response.getStatusCode() == 200) {
            LOGGER.info("EXPECTED RESPONSE 200");
        } else {
            LOGGER.error("UNEXPECTED RESPONSE. EXPECTED '200'");
        }
        LOGGER.info("Making call to: " + baseURI + url);
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());

        endTest(test);

    }

    @Test
    public void retrieveListChatSubscriptionsUser2() {
        String userID = user2;
        int i = 2;

        TestUtils.startNotificationChannel(userTwo, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToChatNotifications(userTwo, chatSubscriptionURL, chatRequestDataConfirmed, apiVersion,
                                               applicationUsername, applicationPassword);

        String test = "Retrieve List of all Chat Subscriptions for User 1";
        startTest(test);

        String cleanUserID = cleanPrefix(userID);
        String url = replace(chatSubscriptionURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("chatSubscriptionList.chatNotificationSubscription.size()",
                                 Matchers.is(1),
                                 "chatSubscriptionList.resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatSubscriptionList.chatNotificationSubscription[0].resourceURL",
                                 StringContains.containsString(cleanUserID),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.callbackData",
                                 IsEqual.equalTo("GSMA1"),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.notifyURL",
                                 IsEqual.equalTo(userTwo.getCallbackURL()),
                                 "chatSubscriptionList.chatNotificationSubscription[0].callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"))
                           .get(url);

        if (response.getStatusCode() == 200) {
            LOGGER.info("EXPECTED RESPONSE 200");
        } else {
            LOGGER.error("UNEXPECTED RESPONSE. EXPECTED '200'");
        }
        LOGGER.info("Making call to: " + baseURI + url);
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());

        endTest(test);

    }

    @Test
    public void retrieveListChatSubscriptionsUser3NoSubscriptions() {
        String userID = user3;
        int i = 3;

        TestUtils.startNotificationChannel(userThree, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        // subscribeToChatNotificationsConfirmed(userID, i);

        String test = "Retrieve List of all Chat Subscriptions for User 3 with no Subscriptions";
        startTest(test);

        String url = replace(chatSubscriptionURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(400)
                           .get(url);

        JsonPath jsonData = response.jsonPath();
        String errorCode = jsonData.get("requestError.serviceException.messageId");
        String errorMessage = jsonData.get("requestError.serviceException.variables[0]");
        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Error Code = " + errorCode);
        LOGGER.info("Error Message = " + errorMessage);

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

    public void setUrlSplit(String urlSplit) {
        this.urlSplit = urlSplit;
    }

}
