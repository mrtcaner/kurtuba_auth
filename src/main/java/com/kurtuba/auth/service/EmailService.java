
package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.EmailDetails;

public interface EmailService {

    void sendMultipartMail(EmailDetails details);

}
