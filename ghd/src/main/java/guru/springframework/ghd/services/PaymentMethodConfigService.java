package guru.springframework.ghd.services;

import guru.springframework.ghd.constants.enums.PaymentEnum;

import java.util.Set;

public interface PaymentMethodConfigService {

    boolean isEnabled(PaymentEnum method);

    Set<PaymentEnum> getEnabledMethods();
}
