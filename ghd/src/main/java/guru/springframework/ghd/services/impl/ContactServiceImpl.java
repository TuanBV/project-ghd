package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.ContactStatus;
import guru.springframework.ghd.dto.contact.ContactRequest;
import guru.springframework.ghd.dto.contact.ContactResponse;
import guru.springframework.ghd.dto.contact.ContactUpdateRequest;
import guru.springframework.ghd.entities.Contact;
import guru.springframework.ghd.mappers.ContactMapper;
import guru.springframework.ghd.repositories.ContactRepository;
import guru.springframework.ghd.services.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;

    @Override
    public Page<ContactResponse> getList(String search, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, sortField, sortDir);
        Page<Contact> contactPage;

        if (StringUtils.hasText(search)) {
            contactPage = contactRepository.findAllByFullNameContainingIgnoreCaseOrPhoneContaining(search, search, pageRequest);
        } else {
            contactPage = contactRepository.findAll(pageRequest);
        }

        return contactPage.map(contactMapper::contactToContactResponse);
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, String sortField, String sortDir) {
        return getPageRequest(pageNumber, pageSize, sortField, sortDir);
    }

    @Override
    public Optional<ContactResponse> getById(String contactId) {
        return contactRepository.findById(contactId).map(contactMapper::contactToContactResponse);
    }


    @Override
    public Optional<ContactResponse> updateById(String contactId, ContactUpdateRequest request) {
        return contactRepository.findById(contactId).map(existingContact -> {
            existingContact.setStatus(ContactStatus.valueOf(request.getStatus()));

            existingContact.setNote(request.getNote());

            return contactMapper.contactToContactResponse(contactRepository.save(existingContact));
        });
    }

    @Override
    public ContactResponse addContact(ContactRequest request) {
        Contact newContact = Contact.builder()
                .phone(request.getPhone())
                .message(request.getMessage())
                .fullName(request.getFullName())
                .status(ContactStatus.NEW)
                .serviceType(request.getServiceType())
                .build();
        return contactMapper.contactToContactResponse(contactRepository.save(newContact));
    }
}