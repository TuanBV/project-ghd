package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.contact.ContactRequest;
import guru.springframework.ghd.dto.contact.ContactResponse;
import guru.springframework.ghd.dto.contact.ContactUpdateRequest;
import guru.springframework.ghd.services.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contact")
public class ContactController extends BaseController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<?> getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(defaultValue = DefaultPage.SIZE_STRING) Integer pageSize,
            @RequestParam(defaultValue = DefaultPage.CREATED_DATE) String sortField,
            @RequestParam(defaultValue = DefaultPage.DESC) String sortDir
    ) {
        Page<ContactResponse> pageContact = contactService.getList(search, sortField, sortDir, pageNumber, pageSize);
        return ok(pageContact);
    }

    @PostMapping
    public ResponseEntity<?> addContact(@Valid @RequestBody ContactRequest contactRequest) {
        ContactResponse savedContact = contactService.addContact(contactRequest);
        return ok(savedContact);
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<?> getById(@PathVariable String contactId) {
        return contactService.getById(contactId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{contactId}")
    public ResponseEntity<?> update(@PathVariable String contactId, @Valid @RequestBody ContactUpdateRequest request) {
        return contactService.updateById(contactId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }
}
