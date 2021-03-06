/* 
 * Copyright 2015 Rhythm & Hues Studios.
 *
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
 */
package com.rhythm.louie.email;

import java.util.Collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author sfong
 */
public class EmailTask implements Runnable {
    private CustomIDMessage msg;

    private EmailTask() throws MessagingException {
        this.msg = new CustomIDMessage(MailProperties.getSession());
    }
    
    public EmailTask(String from, String to, String subject, String body) throws MessagingException {
        this();
        this.msg.addFrom(new InternetAddress[] {new InternetAddress(from)});
        this.msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        this.msg.setSubject(subject);
        this.msg.setText(body);
    }

    public EmailTask(String from, Collection<String> toList, String subject, String body) throws AddressException, MessagingException {
        this();
        this.msg.addFrom(new InternetAddress[] {new InternetAddress(from)});
        
        List<InternetAddress> toAddr = new ArrayList<>();
        for (String to : toList){
            toAddr.add(new InternetAddress(to));
        }
        
        Address[] addresses = toAddr.toArray(new Address[toAddr.size()]);
        this.msg.addRecipients(Message.RecipientType.TO, addresses);
        this.msg.setSubject(subject);
        this.msg.setText(body);
    }
    
    public void addTo(String recipient) throws AddressException, MessagingException {
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
    }
    
    public void setReplyTo(String replyTo) throws AddressException, MessagingException{
        InternetAddress[] addrs = new InternetAddress[1];
        addrs[0] = new InternetAddress(replyTo);
        msg.setReplyTo(addrs);
    }
    
    public void setReplyTo(Collection<String> replyTo) throws AddressException, MessagingException {
        InternetAddress[] addrs = new InternetAddress[replyTo.size()];
        int count = 0;
        for (String recipient : replyTo){
            addrs[count] = new InternetAddress(recipient);
            count++;
        }
        msg.setReplyTo(addrs);
    }

    public void addCC(String cc) throws AddressException, MessagingException {
        msg.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
    }
    
    public void addCCs(Collection<String> cc) throws AddressException, MessagingException {
        InternetAddress[] addrs = new InternetAddress[cc.size()];
        int count = 0;
        for (String recipient : cc){
            addrs[count] = new InternetAddress(recipient);
            count++;
        }
        msg.addRecipients(Message.RecipientType.CC, addrs);
    }
    
    public void addBCC(String bcc) throws AddressException, MessagingException {
        msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
    }
    
    public void addBCCs(Collection<String> bcc) throws AddressException, MessagingException {
        InternetAddress[] addrs = new InternetAddress[bcc.size()];
        int count = 0;
        for (String recipient : bcc){
            addrs[count] = new InternetAddress(recipient);
            count++;
        }
        msg.addRecipients(Message.RecipientType.BCC, addrs);
    }

    public void setInReplyTo(String inReplyTo) throws MessagingException {
        msg.addHeader("In-Reply-To",inReplyTo);
    }

    public void setMessageId(String messageId) throws MessagingException {
        msg.setMessageId(messageId);
    }

    public void setReferences(String references) throws MessagingException {
        msg.addHeader("References",references);
    }
    
    public void setContentType(String contentType) throws MessagingException {
        msg.addHeader("Content-Type", contentType);
    }
    
    public void addHeaders(Map<String, String> xHeaders) throws MessagingException {
        for (Map.Entry<String, String> entry : xHeaders.entrySet()) {
            msg.addHeader(entry.getKey(), entry.getValue());
        }
    }
    
    public void addHeader(String key, String value) throws MessagingException {
        msg.addHeader(key, value);
    }
    
    @Override
    public void run() {
        try {
            Transport.send(msg);
        } catch (MessagingException e) {
            System.out.println(e);
        }
    }
    
    static private class CustomIDMessage extends MimeMessage {
        
        private String messageId;
        
        public CustomIDMessage(Session session) {
            super(session);
        }
        
        public void setMessageId(String id) {
            messageId = id;
        }
        
        @Override
        protected void updateMessageID() throws MessagingException {
            if (messageId != null) {
                setHeader("Message-ID", messageId);
            } else {
                super.updateMessageID();
            }
        }
    }
}
