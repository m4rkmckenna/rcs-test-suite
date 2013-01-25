package com.aepona.rcs.test.common;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

public class TestUtils {

	public static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

	public static void deleteNotificationChannel(
			final TestSubscriber testSubscriber, final String userName,
			String password) {
		LOG.info("Cleaning up test by deleting notification channel = "
				+ testSubscriber.getResourceURL());
		if (StringUtils.isNotBlank(testSubscriber.getResourceURL())) {
			Response response = RestAssured.with().auth().preemptive()
					.basic(userName, password)
					.delete(prepare(testSubscriber.getResourceURL()));
			LOG.info("Response Received = " + response.getStatusCode());
			LOG.info("Body = " + response.asString());
		} else {
			LOG.info("No notification channel to delete!");
		}
	}

	public static Response registerUser(final TestSubscriber testSubscriber,
			final String registerURL, final String apiVersion,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> registerUser : " + testSubscriber + " registerURL : "
				+ registerURL);
		String url = replace(registerURL, apiVersion,
				testSubscriber.getUserID());
		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword).expect().log()
				.ifError().statusCode(204).when().post(url);
		LOG.info("<<< registerUser : " + testSubscriber);
		return response;
	}

	public static Response startNotificationChannel(
			final TestSubscriber testSubscriber,
			final String notificationChannelURL, final String apiVersion,
			final String validLongPoll, final String applicationUsername,
			final String applicationPassword) {
		LOG.info(">>> startNotificationChannel : " + testSubscriber
				+ " notificationChannelURL : " + notificationChannelURL
				+ " validLongPoll : " + validLongPoll);
		String url = replace(notificationChannelURL, apiVersion,
				testSubscriber.getUserID());
		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword)
				.body(validLongPoll).header("accept", "application/json")
				.header("Content-Type", "application/json").expect().log()
				.ifError().statusCode(201).post(url);

		JsonPath jsonData = response.jsonPath();
		testSubscriber.setResourceURL(prepare((String) jsonData
				.get("notificationChannel.resourceURL")));
		testSubscriber.setChannelURL(prepare((String) jsonData
				.get("notificationChannel.channelData.channelURL")));
		testSubscriber.setCallbackURL(prepare((String) jsonData
				.get("notificationChannel.callbackURL")));
		LOG.info("<<< startNotificationChannel : " + testSubscriber);
		return response;
	}

	public static Response subscribeToSession(
			final TestSubscriber testSubscriber,
			final String sessionSubscriptionURL,
			final String sessionRequestData, final String apiVersion,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> subscribeToSession : " + testSubscriber
				+ " sessionSubscriptionURL : " + sessionSubscriptionURL
				+ " sessionRequestData : " + sessionRequestData);
		String url = replace(sessionSubscriptionURL, apiVersion,
				testSubscriber.getUserID());
		String body = requestDataClean(sessionRequestData,
				testSubscriber.getUserID(), testSubscriber.getCallbackURL());
		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword).body(body)
				.expect().log().ifError().statusCode(201).when().post(url);
		JsonPath jsonData = response.jsonPath();
		testSubscriber.setSessionSubscriptionUrl(prepare((String) jsonData
				.getString("sessionSubscription.resourceURL")));
		LOG.info("<<< subscribeToSession : " + testSubscriber);
		return response;
	}

	public static Response subscribeToFileTransfer(
			final TestSubscriber testSubscriber,
			final String fileTransferSubscriptionURL,
			final String fileTransferRequestData, final String apiVersion,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> subscribeToFileTransfer : " + testSubscriber
				+ " fileTransferSubscriptionURL : " + fileTransferSubscriptionURL
				+ " fileTransferRequestData : " + fileTransferRequestData);		
		String requestData = requestDataClean(fileTransferRequestData,
				testSubscriber.getUserID(), testSubscriber.getCallbackURL());
		String url = replace(fileTransferSubscriptionURL, apiVersion,
				testSubscriber.getUserID());

		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword)
				.contentType("application/json").body(requestData).expect()
				.statusCode(201).post(url);

		testSubscriber.setFileTransferSubscriptionUrl(prepare((String) response
				.jsonPath().getString("fileTransferSubscription.resourceURL")));
		LOG.info("<<< subscribeToFileTransfer : " + testSubscriber);
		return response;
	}

	public static Response subscribeToAddressBookNotifications(
			final TestSubscriber testSubscriber,
			final String addressBookSubscriptionURL,
			final String addressBookRequestData, final String apiVersion,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> subscribeToAddressBookNotifications : " + testSubscriber
				+ " addressBookSubscriptionURL : " + addressBookSubscriptionURL
				+ " addressBookRequestData : " + addressBookRequestData);			
		String clientCorrelator = Long.toString(System.currentTimeMillis());
		String callback = testSubscriber.getCallbackURL();
		String requestData = requestDataClean(addressBookRequestData,
				clientCorrelator, callback);
		String url = replace(addressBookSubscriptionURL, apiVersion,
				testSubscriber.getUserID());

		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword)
				.contentType("application/json").body(requestData).expect()
				.log().ifError().statusCode(201).post(url);

		JsonPath jsonData = response.jsonPath();
		testSubscriber.setAddressSubscriptionUrl(prepare((String) jsonData
				.getString("abChangesSubscription.resourceURL")));
		LOG.info("<<< subscribeToAddressBookNotifications : " + testSubscriber);		
		return response;
	}

	public static Response subscribeToChatNotifications(
			final TestSubscriber testSubscriber,
			final String chatSubscriptionURL,
			final String chatRequestDataAdhoc, final String apiVersion,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> subscribeToChatNotifications : " + testSubscriber
				+ " chatSubscriptionURL : " + chatSubscriptionURL
				+ " chatRequestDataAdhoc : " + chatRequestDataAdhoc);			
		String clientCorrelator = Long.toString(System.currentTimeMillis());
		String callback = testSubscriber.getCallbackURL();
		String requestData = requestDataClean(chatRequestDataAdhoc,
				clientCorrelator, callback);
		String url = replace(chatSubscriptionURL, apiVersion,
				testSubscriber.getUserID());
		Response response = RestAssured.given().auth().preemptive()
				.basic(applicationUsername, applicationPassword)
				.contentType("application/json").body(requestData).expect()
				.log().ifError().statusCode(201).post(url);
		JsonPath jsonData = response.jsonPath();
		testSubscriber.setChatSubscriptionUrl(prepare((String) jsonData
				.get("chatNotificationSubscription.resourceURL")));
		LOG.info("<<< subscribeToChatNotifications : " + testSubscriber);
		return response;
	}

	public static void clearPendingNotifications(
			final TestSubscriber testSubscriber,
			final String applicationUsername, final String applicationPassword) {
		LOG.info(">>> clearPendingNotifications : " + testSubscriber);
		if (StringUtils.isNotBlank(testSubscriber.getChannelURL())) {
			RestAssured.given().auth().preemptive()
					.basic(applicationUsername, applicationPassword)
					.post(testSubscriber.getChannelURL());
		}
		LOG.info("<<< clearPendingNotifications : " + testSubscriber);
	}

	public static String replace(String replaceableURL, String apiVersion,
			String userID) {
		return replaceableURL.replace("{apiVersion}", apiVersion).replace(
				"{userID}", userID);
	}

	public static String requestDataClean(String dirtyData, String userID,
			String callback) {
		return dirtyData.replace("{CALLBACK}", callback).replace("{USERID}",
				userID);
	}

	// @Test
	// public void deleteNotificationChannels() {
	// String userID = user1;
	// int i = 1;
	// // createNotificationChannel(userID, i);
	//
	// String test = "List Notification Channel(s) for User 1";
	// startTest(test);
	//
	// String cleanUserID = cleanPrefix(userID);
	// String url = replace(notificationChannelURL, apiVersion, userID);
	//
	// Response response =
	// RestAssured.given().auth().preemptive().basic(applicationUsername,
	// applicationPassword).auth().preemptive().basic(applicationUsername,
	// applicationPassword).header("accept",
	// "application/json").header("Content-Type", "application/json")
	// .expect()
	// .log()
	// .ifError()
	// .statusCode(200)
	// .when()
	// .get(url);
	//
	// if (response.getStatusCode() == 200) {
	// LOGGER.info("EXPECTED RESPONSE 200");
	// } else {
	// LOGGER.error("UNEXPECTED RESPONSE. EXPECTED '200'");
	// }
	// // LOGGER.info(response.jsonPath().prettyPrint());
	// JsonPath jsonData = response.jsonPath();
	// Integer listSize =
	// jsonData.get("notificationChannelList.notificationChannel.size()");
	// LOGGER.info("Log size = " + listSize);
	// for (int listI = 0; listI < listSize.intValue(); listI++) {
	// String resource =
	// jsonData.get("notificationChannelList.notificationChannel[" + listI +
	// "].resourceURL");
	// // LOGGER.info(resource);
	// // resource = prepare(resource);
	// // LOGGER.info(resource);
	// Response deleteResponse =
	// RestAssured.with().auth().preemptive().basic(applicationUsername,
	// applicationPassword).expect().log().ifError().statusCode(204).when().delete(resource);
	// LOGGER.info("Response Received = "+deleteResponse.getStatusCode());
	// }
	// // .body("notificationChannelList.resourceURL",
	// // StringContains.containsString(cleanUserID),
	// // ,
	// // Matchers.is(1),
	// // "notificationChannelList.notificationChannel[0].callbackURL",
	// // IsEqual.equalTo(callbackURL[i]),
	// //
	// "notificationChannelList.notificationChannel[0].channelData.channelURL",
	// // IsEqual.equalTo(channelURL[i]))
	// LOGGER.info("Response Received = " + response.getStatusCode());
	// LOGGER.info("Body = " + response.asString());
	// endTest(test);
	// }
	//
	public static String prepare(String url) {
		return url.replaceAll("%2B", "+").replaceAll("%3A", ":");
	}

}
