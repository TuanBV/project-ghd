package com.example.mcprice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Cac phep toan tien te dung BigDecimal, khong dung double/float o bat ky buoc nao. */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** Trung binh cong so hoc cua danh sach gia hop le, tra ve null neu danh sach rong. */
    public static BigDecimal average(List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal p : prices) {
            sum = sum.add(p);
        }
        return sum.divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Lam tron ve buoc gan nhat (vi du buoc 10000: 4123456 -> 4120000).
     * Su dung HALF_UP tren so lan cua buoc lam tron.
     */
    public static BigDecimal roundToStep(BigDecimal value, BigDecimal step) {
        if (value == null) {
            return null;
        }
        if (step == null || step.signum() <= 0) {
            return value.setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal steps = value.divide(step, 0, RoundingMode.HALF_UP);
        return steps.multiply(step).setScale(0, RoundingMode.HALF_UP);
    }

    /** Phan tram thay doi (newValue - oldValue) / oldValue * 100, tra null neu oldValue <= 0. */
    public static BigDecimal percentChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || newValue == null || oldValue.signum() <= 0) {
            return null;
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
