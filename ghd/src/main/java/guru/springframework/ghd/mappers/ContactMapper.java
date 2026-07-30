package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.contact.ContactResponse;
import guru.springframework.ghd.entities.Contact;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactMapper {
    Contact contactResponseToContact(ContactResponse contactResponse);

    ContactResponse contactToContactResponse(Contact contact);
}
