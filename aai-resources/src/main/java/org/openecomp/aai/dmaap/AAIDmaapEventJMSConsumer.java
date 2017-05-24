/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.dmaap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.MDC;
import org.eclipse.jetty.util.security.Password;
import org.json.JSONException;
import org.json.JSONObject;

import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.util.AAIConstants;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
//import com.att.nsa.mr.client.MRBatchingPublisher;
//import com.att.nsa.mr.client.MRClientFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class AAIDmaapEventJMSConsumer implements MessageListener {

	private static final EELFLogger LOGGER = EELFManager.getInstance().getLogger(AAIDmaapEventJMSConsumer.class);

	private Client httpClient;

	//private MRBatchingPublisher aaiEventPublisher = null;
	private Properties aaiEventProps;
	private String aaiEventUrl = "";

	//private MRBatchingPublisher aaiVceEventPublisher = null;
	private Properties aaiVceEventProps;
	private String aaiVceEventUrl = "";

	public AAIDmaapEventJMSConsumer() throws org.apache.commons.configuration.ConfigurationException {
		//super();
		//try {

			//if (this.aaiEventPublisher == null) {
				//FileReader reader = new FileReader(new File(AAIConstants.AAI_EVENT_DMAAP_PROPS));
				//aaiEventProps = new Properties();
				//aaiEventProps.load(reader);
				//reader.close();
				//aaiEventProps.setProperty("DME2preferredRouterFilePath",
						//AAIConstants.AAI_HOME_ETC_APP_PROPERTIES + "preferredRoute.txt");
				//if (aaiEventProps.getProperty("password") != null
						//&& aaiEventProps.getProperty("password").startsWith("OBF:")) {
					//aaiEventProps.setProperty("password", Password.deobfuscate(aaiEventProps.getProperty("password")));
				//}

				//this.aaiEventPublisher = MRClientFactory.createBatchingPublisher(aaiEventProps);

				//String host = aaiEventProps.getProperty("host");
				//String topic = aaiEventProps.getProperty("topic");
				//String protocol = aaiEventProps.getProperty("Protocol");

				//String username = aaiEventProps.getProperty("username");
				//String password = aaiEventProps.getProperty("password");

				//aaiEventUrl = protocol + "://" + host + "/events/" + topic;
				//httpClient = Client.create();
				//httpClient.addFilter(new HTTPBasicAuthFilter(username, password));
			//}

			//if (this.aaiVceEventProps == null) {
				//FileReader reader = new FileReader(new File(AAIConstants.AAI_EVENT_DMAAP_PROPS));
				//aaiVceEventProps = new Properties();
				//aaiVceEventProps.load(reader);
				//reader.close();
				//aaiVceEventProps.setProperty("DME2preferredRouterFilePath",
						//AAIConstants.AAI_HOME_ETC_APP_PROPERTIES + "preferredRoute.txt");
				//if (aaiVceEventProps.getProperty("password") != null
						//&& aaiVceEventProps.getProperty("password").startsWith("OBF:")) {
					//aaiVceEventProps.setProperty("password",
							//Password.deobfuscate(aaiVceEventProps.getProperty("password")));
				//}
				//aaiVceEventProps.setProperty("topic", "AAI-VCE-INTERFACE-DATA");
				//this.aaiVceEventPublisher = MRClientFactory.createBatchingPublisher(aaiVceEventProps);

				//String host = aaiVceEventProps.getProperty("host");
				//String topic = aaiVceEventProps.getProperty("topic");
				//String protocol = aaiVceEventProps.getProperty("Protocol");

				//aaiVceEventUrl = protocol + "://" + host + "/events/" + topic;

			//}
		//} catch (IOException e) {
			//ErrorLogHelper.logError("AAI_4000", "Error updating dmaap config file for aai event.");
		//}

	}

	@Override
	public void onMessage(Message message) {

		//String jsmMessageTxt = "";
		//String aaiEvent = "";
		//String eventName = "";


		//String environment = "";

		//if (message instanceof TextMessage) {
			//try {
				//jsmMessageTxt = ((TextMessage) message).getText();
				//JSONObject jo = new JSONObject(jsmMessageTxt);

				//if (jo.has("aaiEventPayload")) {
					//aaiEvent = jo.getJSONObject("aaiEventPayload").toString();
				//} else {
					//return;
				//}
				//if (jo.getString("transId") != null) {
					//MDC.put("requestId", jo.getString("transId"));
				//}
				//if (jo.getString("fromAppId") != null) {
					//MDC.put("partnerName", jo.getString("fromAppId"));
				//}
				//if (jo.getString("event-topic") != null) {
					//eventName = jo.getString("event-topic");
				//}

				//LOGGER.info(eventName + "|" + aaiEvent);
				//if (eventName.equals("AAI-EVENT")) {
					//if (!this.sentWithHttp(environment, this.httpClient, this.aaiEventUrl, aaiEvent)) {
						//this.aaiEventPublisher.send(aaiEvent);
					//}
					//LOGGER.info(eventName + "|Event sent.");
				//} else if (eventName.equals("AAI-VCE-INTERFACE-DATA")) {
					//String msg = "";
					//if (!this.sentWithHttp(environment, this.httpClient, this.aaiVceEventUrl, aaiEvent)) {
						//this.aaiVceEventPublisher.send(aaiEvent);
						//msg = this.aaiVceEventPublisher.sendBatchWithResponse().getResponseMessage();
					//}
					//LOGGER.info(eventName + "|Event sent. " + msg);
				//} else {
					//LOGGER.error(eventName + "|Event Topic invalid.");
				//}
			//} catch (java.net.SocketException e) {
				//if (!e.getMessage().contains("Connection reset")) {
					//LOGGER.error("AAI_7304 Error reaching DMaaP to send event. " + aaiEvent, e);
				//}
			//} catch (IOException e) {
				//LOGGER.error("AAI_7304 Error reaching DMaaP to send event. " + aaiEvent, e);
			//} catch (JMSException | JSONException e) {
				//LOGGER.error("AAI_7350 Error parsing aaievent jsm message for sending to dmaap. " + jsmMessageTxt, e);
			//} catch (Exception e) {
				//LOGGER.error("AAI_7350 Error sending message to dmaap. " + jsmMessageTxt, e);
			//}
		//}

	}

	private boolean sentWithHttp(String environment, Client client, String url, String aaiEvent) throws IOException {
		//if (environment.startsWith("dev") || environment.startsWith("testINT") || environment.startsWith("testEXT")) {

			//WebResource webResource = client.resource(url);
			//ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
					//.post(ClientResponse.class, aaiEvent);
			//if (response.getStatus() != 200) {
				//LOGGER.info("Failed : HTTP error code : " + response.getStatus());
				//return false;
			//}
		//} else {
			//return false;
		//}
		return true;
	}
}
