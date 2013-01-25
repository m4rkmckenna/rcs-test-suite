package com.aepona.rcs.test.fileTransfer;

import java.util.Properties;

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

    String lastTest = null;

    Boolean initialised = false;

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
    public void subscribeFileTransfer() {
        String userID = user1;
        String test = "Attempting to Subscribe User " + userOne + " to FileTransfers";
        startTest(test);

        String requestData = requestDataClean(fileTransferRequestData, userID, userOne.getCallbackURL());
        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        LOGGER.info("Request Body = " + requestData);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .statusCode(201)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("File Transfer Subscription URL = "
                + response.jsonPath().getString("fileTransferSubscription.resourceURL"));
        endTest(test);
    }

    @Test
    public void subscribeFileTransfer2() {
        String userID = user2;
        String test = "Attempting to Subscribe User " + userTwo + " to FileTransfers";
        startTest(test);

        String requestData = requestDataClean(fileTransferRequestData, userID, userTwo.getCallbackURL());
        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        LOGGER.info("Request Body = " + requestData);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .statusCode(201)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("File Transfer Subscription URL = "
                + response.jsonPath().getString("fileTransferSubscription.resourceURL"));
        endTest(test);
    }

    @Test
    public void subscribeFileTransfer3() {
        String userID = invalidUser;
        String test = "Attempting to Subscribe Invalid User to FileTransfers";
        startTest(test);

        String requestData = requestDataClean(fileTransferRequestData, userID, userOne.getCallbackURL());
        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        LOGGER.info("Request Body = " + requestData);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .statusCode(401)
                           .post(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void subscribeFileTransfer4() {
        String userID = invalidUser;
        String test = "Attempting to Subscribe Invalid User to FileTransfers";
        startTest(test);

        String requestData = requestDataClean(fileTransferRequestData, userID, userOne.getCallbackURL());
        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        LOGGER.info("Request Body = " + requestData);
        LOGGER.info("URL = " + baseURI + url);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .contentType("application/json")
                           .body(requestData)
                           .expect()
                           .statusCode(400)
                           .post(url);

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Error Message = " + response.jsonPath().getString("requestError.serviceException.variables[0]"));
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

    // *** General Methods ***
    public void initialiseUser(final TestSubscriber testUser) {
        LOGGER.info("Initialising User " + testUser + "");
        TestUtils.registerUser(testUser, registerURL, apiVersion, applicationUsername, applicationPassword);
        TestUtils.startNotificationChannel(testUser, notificationChannelURL, apiVersion, validLongPoll,
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

    private String requestDataClean(String fileTransferRequestData, String userID, String callback) {
        String clean = fileTransferRequestData.replace("{CALLBACK}", callback).replace("{USERID}", userID);
        return clean;
    }

    private String replace(String chatSubscriptionURL, String apiVersion, String userID) {
        return chatSubscriptionURL.replace("{apiVersion}", apiVersion).replace("{userID}", userID);
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
}
