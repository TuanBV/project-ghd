package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.policy.PolicyCustomResponse;
import guru.springframework.ghd.dto.policy.PolicyRequest;
import guru.springframework.ghd.dto.policy.PolicyResponse;
import guru.springframework.ghd.services.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/policy")
public class PolicyController extends BaseController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<?> getPolicies(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "packageName") String field,
            @RequestParam(required = false, defaultValue = DefaultPage.ASC) String sort
    ) {
        List<PolicyResponse> policyList = policyService.getList(name, field, sort);
        return ok(policyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            PolicyCustomResponse response = policyService.getById(id);
            return ok(response);
        } catch (Exception e) {
            return ng("Không tìm thấy gói chính sách: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addPolicy(@RequestBody @Valid PolicyRequest request) {
        try {
            policyService.addPolicy(request);
            return ok(null);
        } catch (Exception e) {
            return ng(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(
            @PathVariable String id,
            @RequestBody @Valid PolicyRequest request
    ) {
        try {
            policyService.updatePolicy(id, request);
            return ok(null);
        } catch (Exception e) {
            return ng(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable String id) {
        try {
            policyService.deletePolicy(id);
            return ok(null);
        } catch (Exception e) {
            return ng(e.getMessage());
        }
    }
}