package com.monitor.service.parameter;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitFormParam {
    private String website;
    private String contact;
    private String summary;
    private String firstName;
    private String lastName;
    //    @Email
    private String email;
    //    @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{4}$", message = "Invalid phone number format")
//    private String phoneNumber;
    private String phone;
    private String linkedin;
    private Attachment letter;
    private Attachment resume;
    private String subject;
    private String role;

    public SubmitFormParam setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getName() {
        return this.getFirstName() + " " + this.getLastName();
    }

    // Extracted method for validating string length
    private void validateStringLength(String field, String fieldName, int maxLength) {
        if (field != null && field.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
    }

    // Simplified validation using Optional and extracted common validations
    public void validate() {
        validateStringLength(website, "Website", 200);
        validateStringLength(contact, "Contact", 200);
        validateStringLength(summary, "Summary", 200);
        validateStringLength(firstName, "FirstName", 200);
        validateStringLength(lastName, "LastName", 200);
        validateStringLength(email, "Email", 200);
        validateStringLength(phone, "Phone Number", 200);
        validateStringLength(linkedin, "LinkedIn Profile", 200);
        validateStringLength(role, "Role", 200);

        if (!ObjectUtils.isEmpty(website)) {
            website = website.toLowerCase(Locale.US).replaceFirst("^(http|https)://", "");
            if (!website.matches("[\\w\\-_]+(\\.\\w+)+(\\w+://)?([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?")) {
                throw new IllegalArgumentException("Invalid website format");
            }
        }

        if (!ObjectUtils.isEmpty(email) && !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (!ObjectUtils.isEmpty(phone) && !phone.matches("^[+| 0-9]+([-()]+[0-9])*$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (!ObjectUtils.isEmpty(linkedin)) {
            linkedin = linkedin.toLowerCase(Locale.US).replaceFirst("^(http|https)://", "");
            if (!linkedin.matches("[\\w\\-_]+(\\.\\w+)+(\\w+://)?([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?")) {
                throw new IllegalArgumentException("Invalid linkedin profile format");
            }
        }
    }

    public void validateJob() {
        Optional.ofNullable(firstName).orElseThrow(() -> new IllegalArgumentException("FirstName can not be null."));
        Optional.ofNullable(lastName).orElseThrow(() -> new IllegalArgumentException("LastName can not be null."));
        Optional.ofNullable(email).orElseThrow(() -> new IllegalArgumentException("EMail can not be null."));
        Optional.ofNullable(role).orElseThrow(() -> new IllegalArgumentException("Job description can not be null"));

        validate();
    }

    public void validateContact() {
        Optional.ofNullable(website).orElseThrow(() -> new IllegalArgumentException("Website can not be null."));
        Optional.ofNullable(contact).orElseThrow(() -> new IllegalArgumentException("Contact can not be null."));
        Optional.ofNullable(summary).orElseThrow(() -> new IllegalArgumentException("Summary can not be null."));

        validate();
    }

    public String[] toArray() {
        return new String[]{this.website, this.contact, this.linkedin, this.phone, this.email, this.role, this.getName(), this.summary};
    }

    public static class Attachment {
        String fileName;
        String md5;
        String type;

        public String getFileName() {
            return Objects.requireNonNullElse(this.fileName, "attachment");
        }

        public String getMd5() {
            return md5;
        }

        public String getType() {
            return Objects.requireNonNullElse(this.type, "plain/txt");
        }
    }
}