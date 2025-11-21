package com.example.pfa.reservation.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class emailUtils {

    @Autowired
    private JavaMailSender emailSender;

    public void sendSimpleMessage(String to, String subject, String text , List<String> list){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("infostayprojectpfa@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        if(list != null && list.size() > 0)
            message.setCc(getCcArray(list));
        emailSender.send(message);
    }

    private String[] getCcArray(List<String> ccList){
        String[] cc = new String[ccList.size()];
        for (int i=0 ; i< ccList.size() ; i++){
            cc[i] =ccList.get(i);
        }
        return cc;
    }

    public void forgotMail(String to,String subject,String password) throws MessagingException {
        MimeMessage message =emailSender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(message,true);
        helper.setFrom("infostayprojectpfa@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        String htmlMssg ="<p><b>Your Login Details :</b><br><b>Email:</b>"+to+"<br><b>Password:</b>"+password+"<br> <a ********* >Link here to login</a></p>";
        message.setContent(htmlMssg,"text/html");
        emailSender.send(message);
    }
}
