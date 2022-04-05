package com.vss;

import com.google.gson.Gson;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Main {

    // theoryExamination 1,exchangingDriverLicence 2,vehicleTranscription 4,vehicleFirstRegistration 8,temporaryPermit 16,
    // vehicleDeactivation 32,driverLicenceDuplicate 64,vehicleRegistrationDuplicate 128,exchangingForeignDriverLicence 256,
    public static final int CODE_SERVICE = 8; // activity code; (ex: vehicleFirstRegistration 8)
    public static final int CODE_MUNICIPALITY = 24; // internal code for municipality; (ex: 24-Maramures)
    public static final int INTERVAL_MILLIS = 90000; // 1.5 minutes in millis
    public static final String REF_DATE = "2022-04-09"; // y-m-d format
    public static final boolean KILL_IF_FOUND = false; // kill process if date has been found;

    public static final boolean OPEN_URL = true; // if false url config can be null
    public static final String URL_TO_OPEN = "https://www.youtube.com/watch?v=Gz2GVlQkn4Q";

    public static final boolean SEND_EMAIL = true; // if false email config can be null
    public static final String EMAIL_FROM = "test-from@test.test";
    public static final String EMAIL_PASS = "password-banana";
    public static final String EMAIL_TO = "test-to@test.test";
    public static final String EMAIL_SMTP_HOST = "mail.test.test";
    public static final String EMAIL_SMTP_PORT = "8889";
    public static final String EMAIL_SUBJECT = "Hi from DRPCIV scan";
    public static final String EMAIL_CONTENT = "We found a free date for ";

    public static void main(String[] args) {
        Gson gson = new Gson();
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    boolean hasDate = doCheck(gson);
                    if (hasDate) {
                        if (OPEN_URL) {
                            openSound();
                        }
                        if (SEND_EMAIL) {
                            sendEmail();
                        }
                        if (KILL_IF_FOUND) {
                            System.out.println("Finished scanning..");
                            timer.cancel();
                            timer.purge();
                        }
                    }
                } catch (Exception ex) {
                    // TODO split exception
                    System.out.println("Exception " + ex);
                }
            }
        }, 0, INTERVAL_MILLIS);
    }

    private static boolean doCheck(Gson gson) throws IOException {
        // TODO move url creation above;
        URL url = new URL("https://www.drpciv.ro/drpciv-booking-api/getAvailableDaysForSpecificService/" + CODE_SERVICE + "/" + CODE_MUNICIPALITY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "application/json");
        String text = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        System.out.println(text);
        List<String> dates = (List<String>) gson.fromJson(text, Object.class);
        for (String item : dates) {
            if (item.startsWith(REF_DATE)) {
                System.out.println("Found date!");
                return true;
            }
        }

        return false;
    }

    private static void openSound() throws URISyntaxException, IOException {
        Desktop d = Desktop.getDesktop();
        d.browse(new URI(URL_TO_OPEN));

        System.out.println("URL opened..");

    }

    private static void sendEmail() throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", EMAIL_SMTP_HOST);
        prop.put("mail.smtp.port", EMAIL_SMTP_PORT);
        prop.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASS);
                    }
                });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(EMAIL_TO)
        );
        message.setSubject(EMAIL_SUBJECT);
        message.setText(EMAIL_CONTENT + REF_DATE);

        Transport.send(message);

        System.out.println("Email sent..");
    }

}
