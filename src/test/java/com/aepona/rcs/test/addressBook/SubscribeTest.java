package com.aepona.rcs.test.addressBook;

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

    @Value("${addressBookSubscriptionURL}")
    protected String addressBookSubscriptionURL;

    @Value("${addressBookRequestData}")
    protected String addressBookRequestData;

    @Value("${validLongPoll}")
    protected String validLongPoll;

    @Value("${notificationChannelURL}")
    protected String notificationChannelURL;

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
    public void subscribeUser1ToAddressBookNotifications() {
        TestUtils.startNotificationChannel(userOne, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);

        String test = "Subscribing User 1 to Address Book Notifications";
        startTest(test);

        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userOne.getCallbackURL();
        String requestData = requestDataClean(addressBookRequestData, clientCorrelator, callback);
        String cleanUserID = cleanPrefix(userOne.getUserID());
        String url = replace(addressBookSubscriptionURL, apiVersion, userOne.getUserID());

        LOGGER.info("Making call to: " + baseURI + url);

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
                           .body("abChangesSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "abChangesSubscription.resourceURL", StringContains.containsString(cleanUserID),
                                 "abChangesSubscription.callbackReference.callbackData", IsEqual.equalTo("GSMA1"),
                                 "abChangesSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(userOne.getCallbackURL()),
                                 "abChangesSubscription.callbackReference.notificationFormat", IsEqual.equalTo("JSON"))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Address Book Subscription URL: " + jsonData.getString("abChangesSubscription.resourceURL"));

        endTest(test);
    }

    @Test
    public void subscribeUser2ToAddressBookNotifications() {
        TestUtils.startNotificationChannel(userTwo, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);

        String test = "Subscribing User 2 to Address Book Notifications";
        startTest(test);

        String clientCorrelator = Long.toString(System.currentTimeMillis());
        String callback = userTwo.getCallbackURL();
        String requestData = requestDataClean(addressBookRequestData, clientCorrelator, callback);
        String cleanUserID = cleanPrefix(userTwo.getUserID());
        String url = replace(addressBookSubscriptionURL, apiVersion, userTwo.getUserID());

        LOGGER.info("Making call to: " + baseURI + url);

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
                           .body("abChangesSubscription.clientCorrelator", IsEqual.equalTo(clientCorrelator),
                                 "abChangesSubscription.resourceURL", StringContains.containsString(cleanUserID),
                                 "abChangesSubscription.callbackReference.callbackData", IsEqual.equalTo("GSMA1"),
                                 "abChangesSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(userTwo.getCallbackURL()),
                                 "abChangesSubscription.callbackReference.notificationFormat", IsEqual.equalTo("JSON"))
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Address Book Subscription URL: " + jsonData.getString("abChangesSubscription.resourceURL"));

        endTest(test);
    }

    @Test
    public void subscribeNonExistantUserToAddressBookNotifications() {
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

    private String requestDataClean(String addressBookRequestData, String clientCorrelator, String callback) {
        String clean =
                addressBookRequestData.replace("{CALLBACK}", callback).replace("{CLIENTCORRELATOR}", clientCorrelator);
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

    public void setAddressBookSubscriptionURL(String addressBookSubscriptionURL) {
        this.addressBookSubscriptionURL = addressBookSubscriptionURL;
    }

    public void setAddressBookRequestData(String addressBookRequestData) {
        this.addressBookRequestData = addressBookRequestData;
    }

    public void setValidLongPoll(String validLongPoll) {
        this.validLongPoll = validLongPoll;
    }

    public void setNotificationChannelURL(String notificationChannelURL) {
        this.notificationChannelURL = notificationChannelURL;
    }

}
