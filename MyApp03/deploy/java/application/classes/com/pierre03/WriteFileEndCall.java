package com.pierre03;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.audium.server.AudiumException;
import com.audium.server.proxy.EndCallInterface;
import com.audium.server.session.CallEndAPI;
import com.audium.server.session.ComponentAPI;
import com.audium.server.session.ReadOnlyList;

public class WriteFileEndCall implements EndCallInterface {

	/*
	The purpose of the On Call End class is to perform any final actions before the caller leaves the application, 
	     such as logging or interfacing with external systems such as billing systems. Note that despite referring to the end of the call, 
	     this actually means the end of the visit to the application.	        
	 */
	@Override
	public void onEndCall(CallEndAPI data) throws AudiumException {
		// The EndCallInterface defines a single method onEndCall which acts as the execution method for the on call end class.
		// This onEndCall method receives a single argument, an instance of CallEndAPI. 
		// This CallEndAPI class belongs to the Session API and is used to access information about the visit to the application that just ended 
		//     (such as how the call ended or the result of the call

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH"); //import from java.text
		Date currDate = new Date(); //import from java.util
		String strCurrDate = sdf.format(currDate);
		String fileName = "D:\\CVP\\CVP-Java\\debug\\"+ data.getApplicationName() + "."+ strCurrDate +".log";		

		data.addToLog("EndCallJava Append to file",fileName);

		StringBuilder logstring = new StringBuilder();

		FileOutputStream fileOut = null; //import from java.io
		File f = null; //import from java.io

		try {
			logstring.append("startdate: "+data.getStartDate()) ;
			logstring.append(",ani:" + data.getAni());
			logstring.append(",dnis:" + data.getDnis());
			logstring.append(",callid:" + data.getSessionData("callid")); logstring.append(",sessionID:" + data.getSessionId());
			logstring.append(",result:" + data.getCallResult());
			logstring.append(",howend:" + data.getHowCallEnded());
			logstring.append("\r\n");

			// ****** DO YOUR LAB HERE *****

			String elementName, exitName;
			ReadOnlyList elementHistory = data.getElementHistory(); //import from com.audium.server.session
			ReadOnlyList exitHistory = data.getExitStateHistory();

			String lastExcCode = (String) data.getSessionData("lastException.code");
			if (lastExcCode != null) {
				data.addToLog("LAST EXC NOT NULL", lastExcCode); // this will be writing to the activity log
			}	else {
				data.addToLog("LAST EXC IS NULL", lastExcCode); // this will be writing to the activity log 
			}
			
			if (data.getCallResult().equalsIgnoreCase("error")) {
				String badElement = elementHistory.lastElement();
				logstring.append(",BadElement:" + badElement);
				
				// This file, only gets generated, every 1 hour
				// If you only want to send an email, no more than 1 per hour:
				f=new File(fileName);
				if (!f.exists()) {
					// sendEmail(data,logstring.toString());
					logstring.append(", sent email");
				}

			}
			logstring.append("Elements---Exit States\r\n");
			for (int i=0; i< exitHistory.size(); i++ ) {
				//get each element name and each exit state name
				elementName = elementHistory.get(i);
				exitName = exitHistory.get(i);
				logstring.append( i + ":" + elementName + "---" + exitName + "\r\n");

				//optional Challenge goes here
				HashMap<String, String> allElementData = data.getAllElementData(elementName); //Quick-Fix: import HashMap from java.util
				if (allElementData.size()>0) {
					logstring.append("\tElementData:\r\n");
					for ( Entry<String, String> entry : allElementData.entrySet() ) {//import java.util.Map.Entry
						logstring.append("\t"+ entry.getKey() + "=" + entry.getValue() + "\r\n");
					}
				}
			}

			f = new File(fileName);
			fileOut = new FileOutputStream(f, true); //this creates the file if necessary; true=append
			fileOut.write(logstring.toString().getBytes());
			fileOut.flush();
		} catch (Exception e) {
			data.logWarning("End Call Class Problem" + e);
		} finally {
			if (fileOut!=null) {
				try {
					fileOut.close();
				} catch (Exception e) {
				}//try - catch
			}//if
		}//finally

	}
	private void sendEmail(ComponentAPI api, String logInfo) throws AudiumException {
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			Session session = (Session)envCtx.lookup("mail/myEmail");

			Properties props = session.getProperties();
			props.put("mail.from", api.getApplicationName()+"@classroom.com");

			Message msg = new MimeMessage(session);
			msg.setSubject("End of Call");
			msg.setSentDate(new Date());
			msg.setFrom();
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("admin@tte.com teacher@tte.com", false));//not strict

			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setText(logInfo );

			Multipart mp = new MimeMultipart();
			mp.addBodyPart(mbp);
			msg.setContent(mp);

			Transport.send(msg);

		} catch (Exception ex) {
			throw new AudiumException(ex.toString());
		}
	}

}
