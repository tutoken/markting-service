package com.monitor.service.parameter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Locale;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
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

    // TODO refactor validation
    public void validate() {
        // Website

        if (!ObjectUtils.isEmpty(website)) {
            website = website.toLowerCase(Locale.US);
            if (!website.matches("[http|https:\\/\\/]*[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?")) {
                throw new IllegalArgumentException("Invalid website format");
            }
            website = website.replaceAll("^(http|https):\\/\\/", "");
        }

        if (!ObjectUtils.isEmpty(email) && !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Phone
        if (!ObjectUtils.isEmpty(phone)&& !phone.matches("^[+| 0-9]+([-()]+[0-9])*$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // LinkedIn
        if (!ObjectUtils.isEmpty(linkedin)) {
            linkedin = linkedin.toLowerCase(Locale.US);
            if (!linkedin.matches("[http|https:\\/\\/]*[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?")) {
                throw new IllegalArgumentException("Invalid linkedin profile format");
            }
            linkedin = linkedin.replaceAll("^(http|https):\\/\\/", "");
        }

        if (summary != null && summary.length() > 200) {
            throw new IllegalArgumentException("Content too long");
        }
        if (website != null && website.length() > 200) {
            throw new IllegalArgumentException("website too long");
        }
        if (contact != null && contact.length() > 200) {
            throw new IllegalArgumentException("contact too long");
        }
        if (firstName != null && firstName.length() > 200) {
            throw new IllegalArgumentException("firstName too long");
        }
        if (lastName != null && lastName.length() > 200) {
            throw new IllegalArgumentException("lastName too long");
        }
        if (email != null && email.length() > 200) {
            throw new IllegalArgumentException("email too long");
        }
        if (phone != null && phone.length() > 200) {
            throw new IllegalArgumentException("phone number too long");
        }
        if (linkedin != null && linkedin.length() > 200) {
            throw new IllegalArgumentException("email address too long");
        }
        if (role != null && role.length() > 200) {
            throw new IllegalArgumentException("Role address too long");
        }

    }

    public void validateJob() {
        Objects.requireNonNull(firstName, "FirstName can not be null.");
        Objects.requireNonNull(lastName, "LastName can not be null.");
        Objects.requireNonNull(email, "EMail can not be null.");
        Objects.requireNonNull(role, "Job description can not be null");

        this.validate();
    }

    public void validateContact() {
        Objects.requireNonNull(website, "Website can not be null.");
        Objects.requireNonNull(contact, "Contact can not be null.");
        Objects.requireNonNull(summary, "Summary can not be null.");

        this.validate();
    }

    public String[] toArray() {
        return new String[]{this.website, this.contact, this.linkedin, this.phone, this.email, this.role, this.firstName + " " + this.lastName, this.summary};
    }

    public static class Attachment {
        String fileName;
        String md5;
        String type;

        public String getFileName() {
            if (this.fileName == null) {
                return "attachment";
            }
            return fileName;
        }

        public String getMd5() {
            return md5;
        }

        public String getType() {
            if (this.type == null) {
                return "plain/txt";
            }
            return type;
        }
    }
}