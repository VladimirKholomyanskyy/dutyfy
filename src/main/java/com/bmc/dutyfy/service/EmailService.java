package com.bmc.dutyfy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${dutyfy.admin.email}")
    private String adminEmail;

    public void sendOffDateReminderEmail(String employeeEmail, String employeeName, int year) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(employeeEmail);
        message.setSubject("Duty Schedule " + year + " - Submit Your Preferred Off Dates");
        message.setText(String.format(
                "Hello %s,\n\n" +
                        "The duty schedule for %d will be created in 7 days.\n" +
                        "Please log in to the system and submit your preferred off dates (maximum 5 days).\n\n" +
                        "If you don't submit your preferences, the system will schedule shifts without considering " +
                        "your preferences.\n\n" +
                        "Best regards,\n" +
                        "Duty Management System",
                employeeName, year
        ));

        try {
            mailSender.send(message);
            System.out.println("Reminder email sent to: " + employeeEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + employeeEmail + ": " + e.getMessage());
        }
    }

    public void sendSchedulingFailureEmail(List<String> warnings, int year) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("URGENT: Duty Schedule " + year + " Creation Failed");

        StringBuilder content = new StringBuilder();
        content.append("The duty schedule creation for ").append(year).append(" has failed.\n\n");
        content.append("Issues encountered:\n");

        for (String warning : warnings) {
            content.append("- ").append(warning).append("\n");
        }

        content.append("\nPlease review the constraints and employee availability, then try creating the schedule " +
                "again.\n\n");
        content.append("Best regards,\n");
        content.append("Duty Management System");

        message.setText(content.toString());

        try {
            mailSender.send(message);
            System.out.println("Failure notification sent to admin: " + adminEmail);
        } catch (Exception e) {
            System.err.println("Failed to send failure notification: " + e.getMessage());
        }
    }

    public void sendSwapRequestEmail(String targetEmployeeEmail, String targetEmployeeName,
                                     String requesterName, String shiftDate, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(targetEmployeeEmail);
        message.setSubject("Shift Swap Request from " + requesterName);
        message.setText(String.format(
                "Hello %s,\n\n" +
                        "%s has requested to swap shifts with you.\n\n" +
                        "Shift Date: %s\n" +
                        "Reason: %s\n\n" +
                        "Please log in to the system to accept or decline this request.\n\n" +
                        "Best regards,\n" +
                        "Duty Management System",
                targetEmployeeName, requesterName, shiftDate, reason
        ));

        try {
            mailSender.send(message);
            System.out.println("Swap request email sent to: " + targetEmployeeEmail);
        } catch (Exception e) {
            System.err.println("Failed to send swap request email: " + e.getMessage());
        }
    }
}