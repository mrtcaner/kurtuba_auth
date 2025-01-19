
package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.EmailDetails;
import org.springframework.stereotype.Service;

@Service
public interface EmailService {

    void sendMultipartMail(EmailDetails details);

}
