package com.crm.korean_pathway_crm.lead.service;

import com.crm.korean_pathway_crm.person.service.PersonContactService;
import com.crm.korean_pathway_crm.person.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeadService {
    private final PersonService personService;
    private final PersonContactService personContactService;
}
