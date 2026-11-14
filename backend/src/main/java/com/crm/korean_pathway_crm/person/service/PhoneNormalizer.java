package com.crm.korean_pathway_crm.person.service;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PhoneNormalizer {
    private static final Pattern VIETNAM_LOCAL = Pattern.compile("^0[1-9][0-9]{8,9}$");
    private static final Pattern VIETNAM_INTERNATIONAL = Pattern.compile("^\\+84[1-9][0-9]{8,9}$");
    private static final Pattern INTERNATIONAL = Pattern.compile("^\\+[1-9][0-9]{8,14}$");

    // Vietnamese national numbers use 0 as the trunk prefix. Store the same
    // number in +84 form regardless of whether the caller supplied 0 or +84.
    public String normalize(String input) {
        if (input == null) {
            throw new AppException(ErrorCode.INVALID_PHONE);
        }

        String phone = input.trim();
        if (VIETNAM_LOCAL.matcher(phone).matches()) {
            return "+84" + phone.substring(1);
        }
        if (phone.startsWith("+84")) {
            if (VIETNAM_INTERNATIONAL.matcher(phone).matches()) {
                return phone;
            }
            throw new AppException(ErrorCode.INVALID_PHONE);
        }
        if (INTERNATIONAL.matcher(phone).matches()) {
            return phone;
        }
        throw new AppException(ErrorCode.INVALID_PHONE);
    }
}
