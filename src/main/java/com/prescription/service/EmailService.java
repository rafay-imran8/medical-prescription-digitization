package com.prescription.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;



    // 2. Access Request Email
    public void sendAccessRequestEmail(String patientEmail, String patientName,
                                       String doctorName, String specialization) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(patientEmail);
            helper.setSubject("New Doctor Access Request");

            String html = String.format("""
                <html><body>
                <h2>New Access Request</h2>
                <p>Dear %s,</p>
                <p>Dr. %s (%s) has requested access to your medical records.</p>
                </body></html>
                """, patientName, doctorName, specialization);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. Access Code Email
    public void sendAccessCodeEmail(String patientEmail, String patientName,
                                    String doctorName, String accessCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(patientEmail);
            helper.setSubject("Your Access Code");

            String html = String.format("""
                <html><body>
                <h2>Access Code</h2>
                <p>Dear %s,</p>
                <p>Your access code for Dr. %s is: <strong>%s</strong></p>
                <p>Valid for 1 hour.</p>
                </body></html>
                """, patientName, doctorName, accessCode);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. Access Activated Email
    public void sendAccessActivatedEmail(String doctorEmail, String doctorName, String patientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(doctorEmail);
            helper.setSubject("Access Activated");

            String html = String.format("""
                <html><body>
                <h2>Access Activated</h2>
                <p>Dear Dr. %s,</p>
                <p>Your access to %s's records has been activated.</p>
                </body></html>
                """, doctorName, patientName);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 5. Access Expired Email
    public void sendAccessExpiredEmail(String doctorEmail, String doctorName, String patientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(doctorEmail);
            helper.setSubject("Access Expired");

            String html = String.format("""
                <html><body>
                <h2>Access Expired</h2>
                <p>Dear Dr. %s,</p>
                <p>Your access to %s's records has expired.</p>
                </body></html>
                """, doctorName, patientName);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    /**
     * 1. Email to user after registration (pending approval)
     */
    public void sendRegistrationPendingEmail(String userEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Registration Received - Pending Verification");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #2563eb; border-bottom: 2px solid #2563eb; padding-bottom: 10px;">
                            ✅ Registration Received
                        </h2>
                        
                        <p>Dear %s,</p>
                        
                        <p>Thank you for registering with our Prescription Management System.</p>
                        
                        <div style="background-color: #dbeafe; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; font-weight: bold; color: #1e40af;">
                                ⏳ Your account is currently under review
                            </p>
                            <p style="margin: 10px 0 0 0; color: #1e3a8a;">
                                Our admin team is verifying your credentials. This usually takes 24-48 hours.
                            </p>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">What happens next?</h3>
                            <ol style="color: #666;">
                                <li>Our team will verify your information</li>
                                <li>Once approved, you'll receive an email with a link to set your password</li>
                                <li>After setting your password, you can log in and start using the system</li>
                            </ol>
                        </div>
                        
                        <div style="background-color: #fef3c7; padding: 15px; border-radius: 8px; border-left: 4px solid #f59e0b; margin: 20px 0;">
                            <p style="margin: 0; color: #92400e;">
                                <strong>⚠️ Important:</strong> You will not be able to log in until your account is approved 
                                and you have set your password.
                            </p>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            If you have any questions, please contact our support team.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """, fullName);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Registration pending email sent to: " + userEmail);

        } catch (Exception e) {
            System.err.println("Failed to send registration pending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 2. Email to admin for new patient registration
     */
    public void sendNewRegistrationNotificationToAdmin(Long userId, String fullName,
                                                       String email, String cnic, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("New Patient Registration - Approval Required");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #dc2626; border-bottom: 2px solid #dc2626; padding-bottom: 10px;">
                            🔔 New Patient Registration
                        </h2>
                        
                        <p>A new patient has registered and is awaiting approval.</p>
                        
                        <div style="background-color: #f3f4f6; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>Name:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Email:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>CNIC:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Role:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>User ID:</strong> %d</p>
                        </div>
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s/admin/registrations" 
                               style="background-color: #2563eb; color: white; padding: 12px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block;">
                                Review Registration
                            </a>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            Please review and approve/reject this registration within 48 hours.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """,
                    fullName, email, cnic, role, userId, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Admin notification sent for user: " + userId);

        } catch (Exception e) {
            System.err.println("Failed to send admin notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 3. Email to doctor after registration (pending verification)
     */
    public void sendDoctorRegistrationPendingEmail(String doctorEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(doctorEmail);
            helper.setSubject("Registration Received - Documents Under Review");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #2563eb; border-bottom: 2px solid #2563eb; padding-bottom: 10px;">
                            ✅ Registration Received
                        </h2>
                        
                        <p>Dear Dr. %s,</p>
                        
                        <p>Thank you for registering as a doctor with our Prescription Management System.</p>
                        
                        <div style="background-color: #dbeafe; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; font-weight: bold; color: #1e40af;">
                                🔍 Your documents are under review
                            </p>
                            <p style="margin: 10px 0 0 0; color: #1e3a8a;">
                                Our admin team is verifying your CNIC and medical license. This usually takes 2-3 business days.
                            </p>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">What we're verifying:</h3>
                            <ul style="color: #666;">
                                <li>CNIC authenticity</li>
                                <li>Medical license validity</li>
                                <li>Professional credentials</li>
                            </ul>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">Next steps:</h3>
                            <ol style="color: #666;">
                                <li>Our team will verify your documents</li>
                                <li>Once approved, you'll receive an email with a link to set your password</li>
                                <li>After setting your password, you can access the doctor portal</li>
                            </ol>
                        </div>
                        
                        <div style="background-color: #fef3c7; padding: 15px; border-radius: 8px; border-left: 4px solid #f59e0b; margin: 20px 0;">
                            <p style="margin: 0; color: #92400e;">
                                <strong>⚠️ Important:</strong> You will not be able to log in until your documents are 
                                verified and you have set your password.
                            </p>
                        </div>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """, fullName);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Doctor registration pending email sent to: " + doctorEmail);

        } catch (Exception e) {
            System.err.println("Failed to send doctor registration email: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Send test email
     */
    public void sendTestEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Test Email - Prescription System");
            helper.setText("This is a test email from the Prescription Management System. If you received this, your email configuration is working correctly!");

            mailSender.send(message);
            System.out.println("Test email sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("Failed to send test email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send test email: " + e.getMessage());
        }
    }

    /**
     * Send access granted email (simple version without code)
     */
    public void sendAccessGrantedEmail(String doctorEmail, String doctorName, String patientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(doctorEmail);
            helper.setSubject("Patient Access Granted - Prescription System");

            String htmlContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Access Granted</h2>
                <p>Dear Dr. %s,</p>
                <p>Patient <strong>%s</strong> has granted you access to their medical records.</p>
                <p>You can now view their prescription history.</p>
            </body>
            </html>
            """, doctorName, patientName);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send access granted email: " + e.getMessage());
        }
    }

    /**
     * Send access revoked email
     */
    public void sendAccessRevokedEmail(String doctorEmail, String doctorName, String patientName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(doctorEmail);
            helper.setSubject("Patient Access Revoked - Prescription System");

            String htmlContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Access Revoked</h2>
                <p>Dear Dr. %s,</p>
                <p>Patient <strong>%s</strong> has revoked your access to their medical records.</p>
            </body>
            </html>
            """, doctorName, patientName);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send access revoked email: " + e.getMessage());
        }
    }

    /**
     * Send admin credentials email
     */
    public void sendAdminCredentialsEmail(String email, String fullName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Admin Account Created - Prescription System");

            String htmlContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Admin Account Created</h2>
                <p>Dear %s,</p>
                <p>An admin/analyst account has been created for you.</p>
                <p><strong>Email:</strong> %s</p>
                <p><strong>Temporary Password:</strong> %s</p>
                <p><strong>⚠️ Please change your password after first login!</strong></p>
            </body>
            </html>
            """, fullName, email, temporaryPassword);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Failed to send admin credentials email: " + e.getMessage());
        }
    }
    /**
     * 4. Email to admin for new doctor registration
     */
    public void sendNewDoctorRegistrationToAdmin(Long userId, String fullName, String email,
                                                 String cnic, String licenseNumber, String documentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("New Doctor Registration - Document Verification Required");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #dc2626; border-bottom: 2px solid #dc2626; padding-bottom: 10px;">
                            🔔 New Doctor Registration - Verification Required
                        </h2>
                        
                        <p>A new doctor has registered and is awaiting document verification.</p>
                        
                        <div style="background-color: #f3f4f6; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0;"><strong>Name:</strong> Dr. %s</p>
                            <p style="margin: 5px 0;"><strong>Email:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>CNIC:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>License Number:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>User ID:</strong> %d</p>
                        </div>
                        
                        <div style="background-color: #fef3c7; padding: 15px; border-radius: 8px; border-left: 4px solid #f59e0b; margin: 20px 0;">
                            <p style="margin: 0; color: #92400e;">
                                <strong>📄 Documents to verify:</strong>
                            </p>
                            <ul style="color: #92400e; margin: 10px 0 0 20px;">
                                <li>CNIC</li>
                                <li>Medical License</li>
                            </ul>
                            <p style="margin: 10px 0 0 0; color: #92400e; font-size: 14px;">
                                Document path: <code>%s</code>
                            </p>
                        </div>
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s/admin/registrations" 
                               style="background-color: #2563eb; color: white; padding: 12px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block;">
                                Review & Verify Documents
                            </a>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            Please verify the documents and approve/reject this registration within 48 hours.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """,
                    fullName, email, cnic, licenseNumber, userId, documentPath, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Admin notification sent for doctor: " + userId);

        } catch (Exception e) {
            System.err.println("Failed to send admin notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 5. Email to user with password setup link (after approval)
     */
    public void sendPasswordSetupEmail(String userEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Account Approved - Set Your Password");

            String setupLink = frontendUrl + "/set-password?token=" + token;

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #16a34a; border-bottom: 2px solid #16a34a; padding-bottom: 10px;">
                            🎉 Account Approved!
                        </h2>
                        
                        <p>Dear %s,</p>
                        
                        <p>Great news! Your account has been approved by our admin team.</p>
                        
                        <div style="background-color: #dcfce7; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; font-weight: bold; color: #166534;">
                                ✅ Your credentials have been verified
                            </p>
                            <p style="margin: 10px 0 0 0; color: #166534;">
                                You're almost ready to start using the system!
                            </p>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">Next Step: Set Your Password</h3>
                            <p style="color: #666;">
                                Click the button below to set your password and activate your account:
                            </p>
                        </div>
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s" 
                               style="background-color: #16a34a; color: white; padding: 14px 40px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block; font-size: 16px;">
                                Set My Password
                            </a>
                        </div>
                        
                        <div style="background-color: #fef3c7; padding: 15px; border-radius: 8px; border-left: 4px solid #f59e0b; margin: 20px 0;">
                            <p style="margin: 0; color: #92400e;">
                                <strong>⚠️ Important:</strong>
                            </p>
                            <ul style="color: #92400e; margin: 10px 0 0 20px;">
                                <li>This link is valid for 24 hours</li>
                                <li>After setting your password, you can log in immediately</li>
                                <li>If the button doesn't work, copy this link: <br>
                                    <code style="font-size: 12px; word-break: break-all;">%s</code>
                                </li>
                            </ul>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            If you didn't request this account, please ignore this email or contact support.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """,
                    fullName, setupLink, setupLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Password setup email sent to: " + userEmail);

        } catch (Exception e) {
            System.err.println("Failed to send password setup email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 6. Email to user after setting password (account activated)
     */
    public void sendAccountActivatedEmail(String userEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Account Activated - Welcome!");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #16a34a; border-bottom: 2px solid #16a34a; padding-bottom: 10px;">
                            🎉 Welcome to Prescription Management System!
                        </h2>
                        
                        <p>Dear %s,</p>
                        
                        <p>Your account is now fully activated and ready to use!</p>
                        
                        <div style="background-color: #dcfce7; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center;">
                            <p style="margin: 0; font-size: 18px; font-weight: bold; color: #166534;">
                                ✅ You can now log in
                            </p>
                        </div>
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s/login" 
                               style="background-color: #2563eb; color: white; padding: 14px 40px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block; font-size: 16px;">
                                Go to Login
                            </a>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">What you can do:</h3>
                            <ul style="color: #666;">
                                <li>Upload and manage prescriptions</li>
                                <li>View AI-powered prescription analysis</li>
                                <li>Track your medical history</li>
                                <li>Manage doctor access to your records</li>
                            </ul>
                        </div>
                        
                        <div style="background-color: #dbeafe; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 0; color: #1e3a8a;">
                                <strong>💡 Tip:</strong> Complete your profile after logging in for a better experience.
                            </p>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            If you have any questions or need help, our support team is here for you.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """,
                    fullName, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Account activated email sent to: " + userEmail);

        } catch (Exception e) {
            System.err.println("Failed to send account activated email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 7. Email to user if registration is rejected
     */
    public void sendRegistrationRejectedEmail(String userEmail, String fullName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Registration Update");

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #dc2626; border-bottom: 2px solid #dc2626; padding-bottom: 10px;">
                            Registration Update
                        </h2>
                        
                        <p>Dear %s,</p>
                        
                        <p>Thank you for your interest in our Prescription Management System.</p>
                        
                        <div style="background-color: #fee2e2; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #dc2626;">
                            <p style="margin: 0; font-weight: bold; color: #991b1b;">
                                We're unable to approve your registration at this time
                            </p>
                            <p style="margin: 10px 0 0 0; color: #991b1b;">
                                <strong>Reason:</strong> %s
                            </p>
                        </div>
                        
                        <div style="margin: 20px 0;">
                            <h3 style="color: #2563eb; margin-bottom: 10px;">What you can do:</h3>
                            <ul style="color: #666;">
                                <li>Review the reason provided above</li>
                                <li>Ensure all information is accurate</li>
                                <li>Contact our support team if you have questions</li>
                                <li>Re-register with correct information</li>
                            </ul>
                        </div>
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s/register" 
                               style="background-color: #2563eb; color: white; padding: 12px 30px; 
                                      text-decoration: none; border-radius: 6px; display: inline-block;">
                                Register Again
                            </a>
                        </div>
                        
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">
                            If you believe this is a mistake or need assistance, please contact our support team.
                        </p>
                        
                        <p style="color: #999; font-size: 12px; margin-top: 20px;">
                            This is an automated message from Prescription Management System.
                        </p>
                    </div>
                </body>
                </html>
                """,
                    fullName, reason, frontendUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("Registration rejected email sent to: " + userEmail);

        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}