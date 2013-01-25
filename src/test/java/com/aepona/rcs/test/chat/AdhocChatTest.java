package com.aepona.rcs.test.chat;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
public class AdhocChatTest {

	private static final String SEND_MESSAGE_STATUS_URL = "sendMessageUrl";

    private static final String RETRIEVE_MESSAGE_STATUS_URL = "retrieveMessageUrl";

    private final Logger LOGGER = LoggerFactory.getLogger(AdhocChatTest.class);

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

    public void initialiseUser(final TestSubscriber testUser) {
		LOGGER.info("Initialising User " + testUser);
        TestUtils.registerUser(testUser, registerURL, apiVersion, applicationUsername, applicationPassword);
        TestUtils.startNotificationChannel(testUser, notificationChannelURL, apiVersion, validLongPoll,
                                           applicationUsername, applicationPassword);
        TestUtils.subscribeToChatNotifications(testUser, chatSubscriptionURL, chatRequestDataAdhoc, apiVersion, applicationUsername, applicationPassword);
        TestUtils.clearPendingNotifications(testUser, applicationUsername, applicationPassword);
		LOGGER.info("User 1 has been Initalised!");
	}

	@Test
	public void sendIMMessageAdhoc() throws JsonGenerationException,
			JsonMappingException, IOException {
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";

		String test = "Send IM Message User 1 - Contact 2 (ADHOC)";
		startTest(test);

		ChatMessage chatMessage = new ChatMessage(text, reportRequest);
		ObjectMapper mapper = new ObjectMapper();
		String requestData = "{\"chatMessage\":"
				+ mapper.writeValueAsString(chatMessage) + "}";
		String encodedUserID = encode(userID);
		String encodedContactID = encode(contactID);
		String session = "adhoc";

		String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID,
				session);
		Response response = RestAssured
				.given().auth().preemptive().basic(applicationUsername, applicationPassword)
				.contentType("application/json")
				.body(requestData)
				.expect()
				.log()
				.ifError()
				.statusCode(201)
				.body("resourceReference.resourceURL",
						StringContains.containsString(encodedUserID
								+ "/oneToOne/" + encodedContactID)).post(url);

		LOGGER.info("Response Received = " + response.getStatusCode());
		LOGGER.info("Body = " + response.asString());
		LOGGER.info("ResourceURL = "+response.jsonPath().getString("resourceReference.resourceURL"));
		endTest(test);
	}

	@Test
	public void checkNotifications() throws JsonGenerationException, JsonMappingException, IOException {
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		String test = "Checking User 2 Notifications for Message arrival";
		startTest(test);
		
		userID = user2;
		contactID = contact1;
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).body(
				"notificationList.messageNotification.chatMessage.text",  Matchers.hasItem(text),
				"notificationList.messageNotification.sessionId",  Matchers.hasItem("adhoc"),
				"notificationList.messageNotification.messageId",  Matchers.notNullValue(),
				"notificationList.messageNotification.senderAddress",  Matchers.hasItem(contactID),
				"notificationList.messageNotification.link",  Matchers.notNullValue()
				).post(userTwo.getChannelURL());

		LOGGER.info("Response Received = " + response.getStatusCode());
		LOGGER.info("Body = " + response.asString());
		LOGGER.info("Message Status URL = " + response.jsonPath().getString("notificationList.messageNotification.link.href"));
		endTest(test);
	}
	
	@Test
	public void checkSenderStatus() throws JsonGenerationException, JsonMappingException, IOException{
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		userID = user2;
		notificationCheck(userTwo);
		
		String test = "Checking the Status for the Sender - User1";
		startTest(test);
		
		LOGGER.info("SEND_MESSAGE_STATUS_URL = " + userOne.getAdditionalProperties().get(SEND_MESSAGE_STATUS_URL));
		userID = user1;
		contactID = contact2;
		Response status = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).body(
				"messageStatusReport.status", Matchers.is("DELIVERED")
				).get(userOne.getAdditionalProperties().get(SEND_MESSAGE_STATUS_URL));
		
		LOGGER.info("Response Received = " + status.getStatusCode());
		LOGGER.info("Body = " + status.asString());
		LOGGER.info("Sender Status = "+status.jsonPath().getString("messageStatusReport.status"));
		endTest(test);
	}
	
	@Test
	public void checkNotifications2() throws JsonGenerationException, JsonMappingException, IOException{
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		userID = user2;
		notificationCheck(userTwo);
		userID = user1;
		senderStatusCheck(userOne);
		
		String test = "Checking IM Notifications for User 1";
		startTest(test);
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).post(userOne.getChannelURL());
		
		LOGGER.info("Response Received = " + response.getStatusCode());
		LOGGER.info("Body = " + response.asString());
		endTest(test);
	}
	
	@Test
	public void sendAdhocIMReply() throws JsonGenerationException, JsonMappingException, IOException{
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		userID = user2;
		notificationCheck(userTwo);
		userID = user1;
		senderStatusCheck(userOne);
		
		String test = "Sending reply message from User 2 to User 1";
		startTest(test);
		
		String replyText = "Hello, User 1! How are you?";
		String replyReportRequest = "Displayed";
		userID = user2;
		contactID = contact1;
		String encodedUserID = encode(userID);
		String encodedContactID = encode(contactID);
		String sessionID = "adhoc";
		String url = replaceExtraLong(sendIMURL, apiVersion, userID, contactID, sessionID);
		
		ChatMessage chatMessage=new ChatMessage(replyText, replyReportRequest);
		ObjectMapper mapper=new ObjectMapper();	
		String jsonRequestData="{\"chatMessage\":"+mapper.writeValueAsString(chatMessage)+"}";
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).contentType("application/json").body(jsonRequestData).expect().log().ifError().statusCode(201).body(
				"resourceReference.resourceURL", StringContains.containsString(encodedUserID+"/oneToOne/"+encodedContactID)
		).post(url);
		
		userOne.getAdditionalProperties().put(SEND_MESSAGE_STATUS_URL, response.jsonPath().getString("resourceReference.resourceURL"));
		LOGGER.info("Response Received = " + response.getStatusCode());
		LOGGER.info("Body = " + response.asString());
		LOGGER.info("ResourceURL = "+userOne.getAdditionalProperties().get(SEND_MESSAGE_STATUS_URL));
		endTest(test);
	}
	
	@Test
	public void checkUser1ReceivedReply() throws JsonGenerationException, JsonMappingException, IOException{
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		userID = user2;
		notificationCheck(userTwo);
		userID = user1;
		senderStatusCheck(userOne);
		
		String replyText = "Hello, User 1! How are you?";
		String replyReportRequest = "Displayed";
		userID = user2;
		contactID = contact1;
		sendAdhocMessage(replyText, replyReportRequest, userTwo, contactID);
		
		sleep();
		userID = user1;
		notificationCheck(userOne);
	}
	
	@Test
	public void checkReplySenderStatus() throws JsonGenerationException, JsonMappingException, IOException{
		String userID = user1;
		String contactID = contact2;
		String text = "Hello, User 2";
		String reportRequest = "Displayed";
		
		sendAdhocMessage(text, reportRequest, userOne, contactID);
		sleep();
		userID = user2;
		notificationCheck(userTwo);
		userID = user1;
		senderStatusCheck(userOne);
		
		String replyText = "Hello, User 1! How are you?";
		String replyReportRequest = "Displayed";
		userID = user2;
		contactID = contact1;
		sendAdhocMessage(replyText, replyReportRequest, userTwo, contactID);
		
		sleep();
		userID = user1;
		notificationCheck(userOne);
		userID = user2;
		senderStatusCheck(userTwo);
	}

	// ********** CLASS SPECIFIC METHODS ************
	private void sendAdhocMessage(String text, String reportRequest,
			final TestSubscriber testSubscriber, String contactID) throws JsonGenerationException,
			JsonMappingException, IOException {
		String test = "Send IM Message - ADHOC";
		startTest(test);

		ChatMessage chatMessage = new ChatMessage(text, reportRequest);
		ObjectMapper mapper = new ObjectMapper();
		String requestData = "{\"chatMessage\":"
				+ mapper.writeValueAsString(chatMessage) + "}";
		String session = "adhoc";

		String url = replaceExtraLong(sendIMURL, apiVersion, testSubscriber.getUserID(), contactID,
				session);
		
		LOGGER.info("sendAdhoc url = " + url);
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).contentType("application/json")
				.body(requestData).expect().log().ifError().statusCode(201)
				.post(url);

		testSubscriber.getAdditionalProperties().put(SEND_MESSAGE_STATUS_URL, TestUtils.prepare(response.jsonPath().getString("resourceReference.resourceURL")));
		LOGGER.info("Response Received = " + response.getStatusCode());
		LOGGER.info("Body = " + response.asString());
		LOGGER.info("url returned = " + testSubscriber.getAdditionalProperties().get(SEND_MESSAGE_STATUS_URL));
		endTest(test);
	}

	private void notificationCheck(final TestSubscriber testSubscriber){
		String test = "Checking User Notifications for Message arrival";
		startTest(test);
		
		Response response = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).post(testSubscriber.getChannelURL());

		LOGGER.info("Response Received = " + response.getStatusCode());
		testSubscriber.getAdditionalProperties().put(RETRIEVE_MESSAGE_STATUS_URL, response.jsonPath().getString("notificationList.messageNotification.link.href"));
		LOGGER.info("Message Status URL = " + testSubscriber.getAdditionalProperties().get(RETRIEVE_MESSAGE_STATUS_URL));
		endTest(test);
	}
	
	private void senderStatusCheck(final TestSubscriber testSubscriber){
		String test = "Checking the Status for the Sender - User1";
		startTest(test);
	
		Response status = RestAssured.given().auth().preemptive().basic(applicationUsername, applicationPassword).expect().log().ifError().statusCode(200).body(
				"messageStatusReport.status", Matchers.is("DELIVERED")
				).get(testSubscriber.getAdditionalProperties().get(SEND_MESSAGE_STATUS_URL));
		
		LOGGER.info("Response Received = " + status.getStatusCode());
		LOGGER.info("Body = " + status.asString());
		LOGGER.info("Sender Status = "+status.jsonPath().getString("messageStatusReport.status"));
		endTest(test);
	}
	
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

	private String replaceExtraLong(String url, String apiVersion,
			String userID, String contactID, String sessionID) {
		return url.replace("{apiVersion}", apiVersion)
				.replace("{userID}", userID).replace("{contactID}", contactID)
				.replace("{sessionID}", sessionID);
	}

	public String encode(String userID) {
		return userID.replaceAll("\\:", "%3A").replaceAll("\\+", "%2B")
				.replaceAll("\\@", "%40");
	}

	public String cleanPrefix(String userID) {
		return userID.replaceAll("tel\\:\\+", "").replaceAll("tel\\:", "")
				.replaceAll("\\+", "").replaceAll("\\:", "");
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

		public ChatSessionInformation(String subject, String originatorAddress,
				String originatorName, String tParticipantAddress,
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
}
