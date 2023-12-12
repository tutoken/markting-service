package com.monitor.service.impl.common;

import com.monitor.service.interfaces.EMailService;
import com.monitor.service.parameter.CommonResponse;
import com.monitor.service.parameter.SubmitFormParam;
import com.monitor.service.parameter.SubmitFormParam.Attachment;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service("emailService")
@Slf4j
public class EMailServiceImpl implements EMailService {

    private static final String[] TITLE = {"Website", "Contact", "Name", "Email", "Phone", "Linkedin", "Role", "Summary"};
    private static final String PROMPT = "The TrueUSD Partnership Application Form receives new applications! Please check soon!";
    @Value("${upload.filepath}")
    private String filepath;
    @Value("${email.from}")
    private String fromMail;
    @Value("${email.to}")
    private List<String> toMail;
    @Value("${email.sendgrid.apikey}")
    private String apiKey;

    @Override
    public CommonResponse submit(SubmitFormParam submitFormParam) {
//        submitFormParam.validate();
        return this.sendEmail(submitFormParam);
    }

    @Override
    public CommonResponse upload(MultipartFile file) {
        try {
            String md5Pass = DigestUtils.md5DigestAsHex(file.getBytes());
            File tempFile = new File(filepath + md5Pass);
            file.transferTo(tempFile);

            return new CommonResponse(true, md5Pass);
        } catch (IOException e) {
            return new CommonResponse(false, e.getMessage());
        }
    }

    private CommonResponse sendEmail(SubmitFormParam submitFormParam) {
        SendGrid sendGrid = new SendGrid(apiKey);

        Mail mail = new Mail();
        mail.setFrom(new Email(fromMail));
        mail.setSubject(submitFormParam.getSubject());

        String content = this.createContent(submitFormParam);

        mail.addContent(new Content("text/plain", content));

        Personalization personalization = new Personalization();
        for (String toAddress : toMail) {
            personalization.addTo(new Email(toAddress));
        }
        mail.addPersonalization(personalization);

        this.addAttachment(mail, submitFormParam.getLetter());
        this.addAttachment(mail, submitFormParam.getResume());

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            if (response.getStatusCode() == 202) {
                return new CommonResponse(true, "");
            }
            log.error("Send email failed: " + response.getBody());
            return new CommonResponse(false, "Send email failed.");
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return new CommonResponse(false, "Send email exception.");
        }
    }

    private void addAttachment(Mail mail, Attachment attachment) {
        if (attachment == null || ObjectUtils.isEmpty(attachment.getMd5())) {
            return;
        }
        Attachments attachments = new Attachments();
        String fileContent = this.encryptToBase64(filepath + attachment.getMd5());
        attachments.setContent(fileContent);
        attachments.setType(attachment.getType());
        attachments.setFilename(attachment.getFileName());
        attachments.setDisposition("attachment");

        mail.addAttachments(attachments);
    }

    public String encryptToBase64(String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            byte[] b = Files.readAllBytes(Paths.get(filePath));
            return Base64.getEncoder().encodeToString(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String createContent(SubmitFormParam submitFormParam) {
        StringBuilder content = new StringBuilder(PROMPT).append("\n");
        for (String s : TITLE) {
            try {
                java.lang.reflect.Method method = SubmitFormParam.class.getMethod("get" + s);
                String value = (String) method.invoke(submitFormParam);
                if (value != null) {
                    content.append("【").append(s).append("】：").append(value).append("\n");
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error(e.getMessage(), e);
            }
        }
        return content.toString();
    }
}
