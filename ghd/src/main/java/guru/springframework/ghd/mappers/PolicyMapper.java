package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.policy.PolicyRequest;
import guru.springframework.ghd.dto.policy.PolicyResponse;
import guru.springframework.ghd.entities.Policy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PolicyMapper {

    Policy policyResponseToPolicy(PolicyResponse policyResponse);

    PolicyResponse policyToPolicyResponse(Policy policy);

    Policy toEntity(PolicyRequest request);
}