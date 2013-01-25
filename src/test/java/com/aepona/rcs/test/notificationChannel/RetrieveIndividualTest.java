package com.aepona.rcs.test.notificationChannel;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
	@Value("${urlSplit}")
	protected String urlSplit;
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
		Properties props = System.getProperties();
		props.put("http.proxyHost", proxyURL);
		props.put("http.proxyPort", proxyPort);
		testUser = new TestSubscriber();
		start();
	}
	
    @After
    public void cleanup() {
        TestUtils.deleteNotificationChannel(testUser, applicationUsername, applicationPassword);
    }   
    
	@Test
	public void readUser1Subscriptions(){
		String userID = user1;
		int i = 1;
		createNotificationChannel(userID, i);
		
		String test = "Read Individual Notification Channel for User 1";
		startTest(test);
		String cleanUserID = cleanPrefix(userID);
		String url = testUser.getResourceURL();
		url = prepare(url);
		LOGGER.info("URL = "+url);	

		Response response = RestAssured.with().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).when().get(url);
			
		LOGGER.info("Response Received = "+response.getStatusCode());
		LOGGER.info("Body = "+response.asString());
		
		endTest(test);
	}
	
	@Test
	public void readUser2Subscriptions(){
		String userID = user2;
		int i = 2;
		createNotificationChannel(userID, i);
		
		String test = "Read Individual Notification Channel for User 1";
		startTest(test);
		String cleanUserID = cleanPrefix(userID);
		String url = testUser.getResourceURL();
		url = prepare(url);
		LOGGER.info("URL = "+url);	

		Response response = RestAssured.with().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).when().get(url);
				
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

	private void createNotificationChannel(String userID, int i){
		String test = "Creating Notification Channel";
		startTest(test);
		String cleanUserID = cleanPrefix(userID);
		String url = replace(notificationChannelURL, apiVersion, userID);

		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).auth().preemptive().basic(applicationUsername, applicationPassword).body(validLongPoll).header("accept", "application/json").header("Content-Type", "application/json").expect()
				.log().ifError().statusCode(201).post(url);
		
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
	
	private String replace(String notificationChannelURL, String apiVersion,
			String userID) {
		String newURL = notificationChannelURL.replace("{apiVersion}",
				apiVersion).replace("{userID}", userID);
		return newURL;
	}
	
	public String cleanPrefix(String userID) {
		return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "")
				.replaceAll("\\+", "").replaceAll("\\:", "");
	}
	
	private String prepare(String url) {
		return url.replaceAll(urlSplit, "").replaceAll("%2B", "+").replaceAll("%3A", ":");
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
	
	public void setUrlSplit(String urlSplit) {
		this.urlSplit = urlSplit;
	}

}
