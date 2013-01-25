package com.aepona.rcs.test.general;

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
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring/application-context.xml" })
public class UnsubscribeTest {

	private final Logger LOGGER = LoggerFactory.getLogger(UnsubscribeTest.class);

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
	@Value("${sessionSubscriptionURL}")
	protected String sessionSubscriptionURL;
	@Value("${validLongPoll}")
	protected String validLongPoll;
	@Value("${notificationChannelURL}")
	protected String notificationChannelURL;
	@Value("${sessionRequestData}")
	protected String sessionRequestData;
	@Value("${urlSplit}")
	protected String urlSplit;

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
	public void unsubscribeUser1(){
		String userID = user1;
		int i = 1;
		
		TestUtils.startNotificationChannel(userOne, notificationChannelURL, apiVersion, validLongPoll, applicationUsername, applicationPassword);
		TestUtils.subscribeToSession(userOne, sessionSubscriptionURL, sessionRequestData, apiVersion, applicationUsername, applicationPassword);
		
		String test = "Unsubscribing User 1 from Session Notifications";
		startTest(test);
		
		String url = userOne.getSessionSubscriptionUrl();
		url = prepare(url);
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(204).when().delete(url);

		LOGGER.info("Response Received = "+response.getStatusCode());
		LOGGER.info("Body = "+response.asString());
		endTest(test);
	}
	
	@Test
	public void unsubscribeUser2(){
		String userID = user2;
		int i = 2;
		
        TestUtils.startNotificationChannel(userTwo, notificationChannelURL, apiVersion, validLongPoll, applicationUsername, applicationPassword);
        TestUtils.subscribeToSession(userTwo, sessionSubscriptionURL, sessionRequestData, apiVersion, applicationUsername, applicationPassword);

		
		String test = "Unsubscribing User 2 from Session Notifications";
		startTest(test);
		
        String url = userTwo.getSessionSubscriptionUrl();
		url = prepare(url);
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(204).when().delete(url);
		
		LOGGER.info("Response Received = "+response.getStatusCode());
		LOGGER.info("Body = "+response.asString());
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

	private String prepare(String url) {
		return url.replaceAll(urlSplit, "").replaceAll("%2B", "+").replaceAll("%3A", ":");
	}
	
	public String cleanPrefix(String userID) {
		return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "")
				.replaceAll("\\+", "").replaceAll("\\:", "");
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

	public void setSessionSubscriptionURL(String sessionSubscriptionURL) {
		this.sessionSubscriptionURL = sessionSubscriptionURL;
	}

	public void setValidLongPoll(String validLongPoll) {
		this.validLongPoll = validLongPoll;
	}

	public void setNotificationChannelURL(String notificationChannelURL) {
		this.notificationChannelURL = notificationChannelURL;
	}

	public void setSessionRequestData(String sessionRequestData) {
		this.sessionRequestData = sessionRequestData;
	}
	
	public void setUrlSplit(String urlSplit) {
		this.urlSplit = urlSplit;
	}
}
