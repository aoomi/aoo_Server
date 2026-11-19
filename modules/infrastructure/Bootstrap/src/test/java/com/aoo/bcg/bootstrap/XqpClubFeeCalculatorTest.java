package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XqpClubFeeCalculatorTest {
    @Test void stageChargesOnlyTiedBigWinnersAndSplitsConfiguredCost() {
        var result = XqpClubFeeCalculator.calculate(2,
                List.of(new XqpClubFeeCalculator.Condition(bd("5"), bd("10"), bd("2"))),
                Map.of(84L, bd("9"), 85L, bd("-9")));
        assertEquals(Map.of(84L, bd("10.00")), result.playerCharges());
        assertEquals(bd("8.00"), result.roomFee());
        assertEquals(bd("2.00"), result.guarantee());
    }

    @Test void aaChargesEveryPlayerAndKeepsGuaranteeSeparate() {
        var result = XqpClubFeeCalculator.calculate(5,
                List.of(new XqpClubFeeCalculator.Condition(bd("-9999999"), bd("10"), bd("2"))),
                Map.of(84L, bd("9"), 85L, bd("-9")));
        assertEquals(Map.of(84L, bd("5.00"), 85L, bd("5.00")), result.playerCharges());
        assertEquals(bd("8.00"), result.roomFee());
        assertEquals(bd("2.00"), result.guarantee());
    }

    @Test void ratioUsesXqpIntegerCentRounding() {
        var result = XqpClubFeeCalculator.calculate(1,
                List.of(new XqpClubFeeCalculator.Condition(bd("-9999999"), bd("10"), bd("0"))),
                Map.of(84L, bd("9"), 85L, bd("-9")));
        assertEquals(Map.of(84L, bd("0.45")), result.playerCharges());
        assertEquals(bd("0.45"), result.roomFee());
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
