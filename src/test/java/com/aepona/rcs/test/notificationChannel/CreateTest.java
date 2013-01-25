package com.aepona.rcs.test.notificationChannel;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
@ContextConfiguration(locations = {"/spring/application-context.xml"})
public class CreateTest {

    private final Logger LOGGER = LoggerFactory.getLogger(CreateTest.class);

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

    private Boolean initialised = false;

    private String lastTest = null;

    private TestSubscriber testUser;

    @Before
    public void setup() {
        LOGGER.info("Proxy URL: " + proxyURL);
        Properties props = System.getProperties();
        props.put("http.proxyHost", proxyURL);
        props.put("http.proxyPort", proxyPort);
        start();
        testUser = new TestSubscriber();
    }

    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(testUser, applicationUsername, applicationPassword);
    }

    @Test
    public void subscribeUser1() {
        String test = "Subscribe User 1 to Notifications";
        startTest(test);
        String userID = user1;
        String cleanUserID = cleanPrefix(userID);
        String url = replace(notificationChannelURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(validLongPoll)
                           .header("accept", "application/json")
                           .header("Content-Type", "application/json")
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("notificationChannel.resourceURL", StringContains.containsString(cleanUserID),
                                 "notificationChannel.callbackURL", StringContains.containsString(cleanUserID),
                                 "notificationChannel.channelData.channelURL",
                                 StringContains.containsString(cleanUserID))
                           .when()
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        testUser.setUserID(userID);
        testUser.setResourceURL((String)jsonData.get("notificationChannel.resourceURL"));
        testUser.setChannelURL((String)jsonData.get("notificationChannel.channelData.channelURL"));
        testUser.setCallbackURL((String)jsonData.get("notificationChannel.callbackURL"));

        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Resource URL: " + testUser.getResourceURL());
        LOGGER.info("Channel URL: " + testUser.getChannelURL());
        LOGGER.info("Callback URL: " + testUser.getCallbackURL());
        endTest(test);
    }

    @Test
    public void subscribeUser2() {
        String test = "Subscribe User 2 to Notifications";
        startTest(test);
        String userID = user2;
        String cleanUserID = cleanPrefix(userID);
        String url = replace(notificationChannelURL, apiVersion, userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .body(validLongPoll)
                           .header("accept", "application/json")
                           .header("Content-Type", "application/json")
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(201)
                           .body("notificationChannel.resourceURL", StringContains.containsString(cleanUserID),
                                 "notificationChannel.callbackURL", StringContains.containsString(cleanUserID),
                                 "notificationChannel.channelData.channelURL",
                                 StringContains.containsString(cleanUserID))
                           .when()
                           .post(url);

        JsonPath jsonData = response.jsonPath();
        testUser.setUserID(userID);
        testUser.setResourceURL((String)jsonData.get("notificationChannel.resourceURL"));
        testUser.setChannelURL((String)jsonData.get("notificationChannel.channelData.channelURL"));
        testUser.setCallbackURL((String)jsonData.get("notificationChannel.callbackURL"));

        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        LOGGER.info("Resource URL: " + testUser.getResourceURL());
        LOGGER.info("Channel URL: " + testUser.getChannelURL());
        LOGGER.info("Callback URL: " + testUser.getCallbackURL());
        endTest(test);
    }

    @Test
    public void subscribeInvalidUser() {
        String test = "Subscribe Non-Existant User to Notifications";
        startTest(test);
        String userID = invalidUser;
        String url = replace(notificationChannelURL, apiVersion, userID);

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
                           .when()
                           .post(url);

        LOGGER.info("Response received = " + response.getStatusCode());
        LOGGER.info("Body received = " + response.asString());
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

    private String replace(String notificationChannelURL, String apiVersion, String userID) {
        String newURL = notificationChannelURL.replace("{apiVersion}", apiVersion).replace("{userID}", userID);
        LOGGER.info("Register URL = "+baseURI + newURL);
        return newURL;
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

    public void setApplicationUsername(String applicationUsername) {
        this.applicationUsername = applicationUsername;
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

    public String cleanPrefix(String userID) {
        return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "").replaceAll("\\+", "").replaceAll("\\:", "");
    }
}
