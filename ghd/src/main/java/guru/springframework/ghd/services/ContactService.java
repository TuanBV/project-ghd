package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.contact.ContactRequest;
import guru.springframework.ghd.dto.contact.ContactResponse;
import guru.springframework.ghd.dto.contact.ContactUpdateRequest;
import org.springframework.data.domain.Page;

import java.nio.channels.FileChannel;
import java.util.*;

public interface ContactService {

    Page<ContactResponse> getList(String search, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<ContactResponse> getById(String contactId);


    ContactResponse addContact(ContactRequest contactRequest);

    Optional<ContactResponse> updateById(String contactId, ContactUpdateRequest request);
}