package guru.springframework.ghd.services;


import guru.springframework.ghd.dto.policy.PolicyCustomResponse;
import guru.springframework.ghd.dto.policy.PolicyRequest;
import guru.springframework.ghd.dto.policy.PolicyResponse;
import jakarta.validation.Valid;

import java.util.*;

public interface PolicyService {

    void addPolicy(PolicyRequest request);

    List<PolicyResponse> getList(String name, String field, String sort);

    List<PolicyResponse> getAll();

    void updatePolicy(String id, @Valid PolicyRequest request);

    void deletePolicy(String id);

    PolicyCustomResponse getById(String id);

    PolicyResponse getPolicyById(String id);
}
