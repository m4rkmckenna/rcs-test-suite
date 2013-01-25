package com.aepona.rcs.test.fileTransfer;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
public class RetrieveIndividualTest {

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
    public void retrieveIndividualSubscription() {
        String userID = user1;
        int i = 1;

        String test = "Retrieving Individual Subscription for User " + i;
        startTest(test);

        String url = userOne.getFileTransferSubscriptionUrl();
        url = prepare(url);
        String cleanUserID = cleanPrefix(userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("fileTransferSubscription.resourceURL", StringContains.containsString(cleanUserID),
                                 "fileTransferSubscription.callbackReference.callbackData", IsEqual.equalTo(userID),
                                 "fileTransferSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(userOne.getCallbackURL()),
                                 "fileTransferSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"))
                           .get(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void retrieveIndividualSubscription2() {
        String userID = user2;
        int i = 2;

        String test = "Retrieving Individual Subscription for User " + i;
        startTest(test);

        String url = userTwo.getFileTransferSubscriptionUrl();
        url = prepare(url);
        String cleanUserID = cleanPrefix(userID);

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(200)
                           .body("fileTransferSubscription.resourceURL", StringContains.containsString(cleanUserID),
                                 "fileTransferSubscription.callbackReference.callbackData", IsEqual.equalTo(userID),
                                 "fileTransferSubscription.callbackReference.notifyURL",
                                 IsEqual.equalTo(userTwo.getCallbackURL()),
                                 "fileTransferSubscription.callbackReference.notificationFormat",
                                 IsEqual.equalTo("JSON"))
                           .get(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        LOGGER.info("Body = " + response.asString());
        endTest(test);
    }

    @Test
    public void retrieveIndividualSubscription3() {
        String userID = invalidUser;
        int i = 0;

        String test = "Retrieving Individual Subscription for User " + i;
        startTest(test);

        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        url = url + "/" + userID;

        Response response =
                RestAssured.given()
                           .auth()
                           .preemptive()
                           .basic(applicationUsername, applicationPassword)
                           .expect()
                           .log()
                           .ifError()
                           .statusCode(401)
                           .get(url);

        LOGGER.info("Response Received = " + response.getStatusCode());
        endTest(test);
    }

    @Test
    public void retrieveIndividualSubscription4() {
        String userID = invalidUser;
        int i = 0;

        String test = "Retrieving Individual Subscription for User " + i;
        startTest(test);

        String url = replace(fileTransferSubscriptionURL, apiVersion, userID);
        url = url + "/" + userID;

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

        LOGGER.info("Received Response = " + response.getStatusCode());
        LOGGER.info("Error Message = " + response.jsonPath().getString("requestError.serviceException.variables[0]"));
        endTest(test);
    }

    // *** Helper Methods ***

    // *** General Methods ***

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
}
