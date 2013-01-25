package com.aepona.rcs.test.common;

import java.util.HashMap;
import java.util.Map;

public class TestSubscriber {

    private String userID;

    private String resourceURL;

    private String callbackURL;

    private String channelURL;

    private String fileTransferSubscriptionUrl;

    private String sessionSubscriptionUrl;

    private String addressSubscriptionUrl;

    private String chatSubscriptionUrl;

    private Map<String, String> additionalProperties = new HashMap<String, String>();

    public String getUserID() {
        return userID;
    }

    public void setUserID(final String userID) {
        this.userID = userID;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(final String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public String getCallbackURL() {
        return callbackURL;
    }

    public void setCallbackURL(final String callbackURL) {
        this.callbackURL = callbackURL;
    }

    public String getChannelURL() {
        return channelURL;
    }

    public void setChannelURL(final String channelURL) {
        this.channelURL = channelURL;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public String getFileTransferSubscriptionUrl() {
        return fileTransferSubscriptionUrl;
    }

    public void setFileTransferSubscriptionUrl(final String fileTransferSubscriptionUrl) {
        this.fileTransferSubscriptionUrl = fileTransferSubscriptionUrl;
    }

    public String getSessionSubscriptionUrl() {
        return sessionSubscriptionUrl;
    }

    public void setSessionSubscriptionUrl(final String sessionSubscriptionUrl) {
        this.sessionSubscriptionUrl = sessionSubscriptionUrl;
    }

    public String getAddressSubscriptionUrl() {
        return addressSubscriptionUrl;
    }

    public void setAddressSubscriptionUrl(final String addressSubscriptionUrl) {
        this.addressSubscriptionUrl = addressSubscriptionUrl;
    }

    public String getChatSubscriptionUrl() {
        return chatSubscriptionUrl;
    }

    public void setChatSubscriptionUrl(final String chatSubscriptionUrl) {
        this.chatSubscriptionUrl = chatSubscriptionUrl;
    }

    @Override
    public String toString() {
        return "TestSubscriber [userID=" + userID + ", resourceURL=" + resourceURL + ", callbackURL=" + callbackURL
                + ", channelURL=" + channelURL + ", fileTransferSubscriptionUrl=" + fileTransferSubscriptionUrl
                + ", sessionSubscriptionUrl=" + sessionSubscriptionUrl + ", addressSubscriptionUrl="
                + addressSubscriptionUrl + ", chatSubscriptionUrl=" + chatSubscriptionUrl + ", additionalProperties="
                + additionalProperties + "]";
    }

}
