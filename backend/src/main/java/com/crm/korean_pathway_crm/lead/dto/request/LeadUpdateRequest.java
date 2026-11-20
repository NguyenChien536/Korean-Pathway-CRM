package com.crm.korean_pathway_crm.lead.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeadUpdateRequest {
    UUID sourceId;
    @Size(max = 150)
    String campaignName;
    @DecimalMin(value = "0.00", message = "Ngân sách không được âm")
    @Digits(integer = 12, fraction = 2,
            message = "Ngân sách tối đa 12 chữ số nguyên và 2 chữ số thập phân")
    BigDecimal estimatedBudget;
}
