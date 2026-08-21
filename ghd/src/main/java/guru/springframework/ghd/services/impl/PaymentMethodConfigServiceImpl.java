package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.PaymentEnum;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.services.PaymentMethodConfigService;
import guru.springframework.ghd.services.SysParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentMethodConfigServiceImpl implements PaymentMethodConfigService {

    private static final String KEY_PREFIX = "payment.";
    private static final String KEY_SUFFIX = ".enabled";

    private final SysParamService sysParamService;

    @Override
    public boolean isEnabled(PaymentEnum method) {
        // sys_param cho payment method được seed đủ 4 key qua V30 - nếu thiếu key
        // (chưa chạy migration/xoá nhầm), mặc định BẬT để không vô tình khoá hết
        // phương thức thanh toán của khách vì thiếu cấu hình.
        return sysParamService.getByKey(toKey(method))
                .map(SysParamResponse::getParamValue)
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    @Override
    public Set<PaymentEnum> getEnabledMethods() {
        Set<PaymentEnum> enabled = EnumSet.noneOf(PaymentEnum.class);
        Arrays.stream(PaymentEnum.values()).filter(this::isEnabled).forEach(enabled::add);
        return enabled;
    }

    private String toKey(PaymentEnum method) {
        return KEY_PREFIX + method.name().toLowerCase() + KEY_SUFFIX;
    }
}
