package com.example.mygvp.utils;

import android.os.AsyncTask;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private final String username;
    private final String password;

    public interface EmailListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public EmailSender(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void sendEmail(String toEmail, String subject, String body, EmailListener listener) {
        new SendEmailTask(toEmail, subject, body, listener).execute();
    }

    private class SendEmailTask extends AsyncTask<Void, Void, Exception> {
        private final String toEmail;
        private final String subject;
        private final String body;
        private final EmailListener listener;

        SendEmailTask(String toEmail, String subject, String body, EmailListener listener) {
            this.toEmail = toEmail;
            this.subject = subject;
            this.body = body;
            this.listener = listener;
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            try {
                MimeMessage message = new MimeMessage(session);
                
                // 1. Set Display Name as "MyGVP Team"
                try {
                    message.setFrom(new InternetAddress(username, "MyGVP Team"));
                } catch (UnsupportedEncodingException e) {
                    message.setFrom(new InternetAddress(username));
                }
                
                // 2. Add Reply-To as a non-existent address or different address
                // message.setReplyTo(new javax.mail.Address[] { new InternetAddress("no-reply@gvpcdpgc.edu.in") });
                
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                
                // 3. Update body to include a "Do Not Reply" notice
                String finalBody = body + "\n\n----------------------------\nThis is an automated message. Please do not reply to this email.";
                message.setText(finalBody);
                
                Transport.send(message);
                return null;
            } catch (MessagingException e) {
                Log.e("EmailSender", "Error sending email", e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (listener != null) {
                if (e == null) {
                    listener.onSuccess();
                } else {
                    listener.onFailure(e);
                }
            }
        }
    }
}
