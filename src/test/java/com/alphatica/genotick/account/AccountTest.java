package com.alphatica.genotick.account;

import com.alphatica.genotick.data.DataSetName;
import com.alphatica.genotick.genotick.Prediction;
import com.alphatica.genotick.ui.CsvOutput;
import com.alphatica.genotick.ui.UserOutput;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AccountTest {
    
    private Account account;
    private BigDecimal initial;

    private final DataSetName name1 = new DataSetName("one");
    private final double price1 = 100;
    private final DataSetName name2 = new DataSetName("two");
    private final double price2 = 1000;
    private final Map<DataSetName, Double> map1 = Collections.singletonMap(name1, price1);
    private final Map<DataSetName, Double> map2 = buildMap2();
    private MockUserOutput output;
    private MockProfitRecorder profitRecorder;

    private Map<DataSetName,Double> buildMap2() {
        Map<DataSetName, Double> map = new HashMap<>();
        map.put(name1, price1);
        map.put(name2, price2);
        return map;
    }

    @BeforeMethod
    public void init() throws IOException {
        initial = BigDecimal.valueOf(1_000_000);
        output = new MockUserOutput();
        profitRecorder = new MockProfitRecorder(output);
        account = new Account(initial, output, profitRecorder);
    }

    @Test
    public void reportsNegativeQuantityIfPredictionDown() {
        account.addPendingOrder(name1, Prediction.DOWN);
        output.clear();
        account.openTrades(map1);
        assertTrue(output.quantity.doubleValue() < 0);
    }

    @Test
    public void reportsAccountOpening() {
        compare(initial, output.accountOpening);
    }

    @Test
    public void reportsAccountClosing() {
        Account acc = new Account(initial, output, profitRecorder);
        output.clear();
        acc.closeAccount();
        compare(initial, output.accountClosing);
        assertEquals(output.message, null);
    }

    @Test
    public void reportsPendingTrade() {
        Prediction prediction = Prediction.UP;
        account.addPendingOrder(name1, prediction);
        assertEquals(name1, output.name);
        assertEquals(prediction, output.prediction);
    }

    @Test
    public void reportsOpeningTrade() {
        Prediction prediction = Prediction.UP;
        account.addPendingOrder(name1, prediction);
        output.clear();
        account.openTrades(map1);
        assertEquals(name1, output.name);
        BigDecimal price = BigDecimal.valueOf(map1.get(name1));
        assertEquals(initial.divide(price, MathContext.DECIMAL128), output.quantity);
        assertEquals(map1.get(name1), output.price);
    }

    @Test
    public void reportsClosingTrade() {
        Prediction prediction = Prediction.UP;
        account.addPendingOrder(name1, prediction);
        account.openTrades(map1);
        output.clear();
        account.closeTrades(map1);
        BigDecimal closePrice = BigDecimal.valueOf(map1.get(name1));
        assertEquals(name1, output.name);
        compare(initial.divide(closePrice, MathContext.DECIMAL128), output.quantity);
        compare(closePrice, output.closePrice);
        compare(BigDecimal.ZERO, output.profit);
        compare(initial, output.cash);
    }

    @Test
    public void returnsCorrectValuesWithTrades() {
        account.addPendingOrder(name1, Prediction.UP);
        account.addPendingOrder(name2, Prediction.DOWN);
        account.addPendingOrder(new DataSetName("no_such_market"), Prediction.UP);
        account.openTrades(map2);
        compare(account.getValue(), initial);
    }

    @Test
    public void closesTrades() {
        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);
        output.clear();
        account.closeAccount();
        assertEquals(profitRecorder.addTradeResultCount, map1.size());
        assertEquals(initial, account.getCash());
    }

    @Test
    public void ignoresOpenTradesIfNoPendingOrders() {
        BigDecimal cashStart = account.getCash();
        account.openTrades(map1);
        BigDecimal cashEnd = account.getCash();
        assertEquals(cashStart, cashEnd);
    }

    @Test
    public void shouldIgnorePendingOrderIfOut() {
        account.addPendingOrder(name1, Prediction.OUT);
        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);
        account.closeTrades(map1);
        output.clear();
        account.closeAccount();
        assertEquals(initial, account.getCash());
    }

    @Test
    public void valueUpIfTwoMarketsUp() {
        double profit = 1.05;
        account.addPendingOrder(name1, Prediction.UP);
        account.addPendingOrder(name2, Prediction.UP);
        account.openTrades(map2);
        Map<DataSetName, Double> map = new HashMap<>();
        map.put(name1, price1 * profit);
        map.put(name2, price2 * profit);
        account.closeTrades(map);
        output.clear();
        account.closeAccount();
        BigDecimal closed = account.getCash();
        compare(closed, initial.multiply(BigDecimal.valueOf(profit)));
        assertTrue(profitRecorder.calledWinRate);
    }

    @Test
    public void valueUpIfOneMarketDown() {
        double change = 0.05;
        double finalPrice = price1 * (1 - change);
        BigDecimal finalAccount = initial.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(change)));
        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);
        account.closeTrades(Collections.singletonMap(name1, finalPrice));
        compare(finalAccount, account.getCash());
    }

    @Test
    public void callsProfitRecordedWithRightValue() {
        account.addPendingOrder(name1, Prediction.UP);
        account.openTrades(map1);
        double change = 0.05;
        double finalPrice = price1 * (1 + change);
        account.closeTrades(Collections.singletonMap(name1, finalPrice));
        assertEquals(profitRecorder.profit, change * initial.doubleValue());
    }

    /*
    valueUpIfTwoMarketsDown
    valueUpIfOneMarketUpOneDown
    valueDownIfOneMarketDown
    valueDownIfTwoMarketsUp
    valueDownIfTwoMarketsDown
    valueDownIfOneMarketUpOneDown
     */

    @Test
    public void canOpenConsecutiveTrades() {
        account.addPendingOrder(name1, Prediction.UP);
        account.openTrades(map1);
        account.closeTrades(map1);

        account.addPendingOrder(name1, Prediction.UP);
        account.openTrades(map1);
        account.closeTrades(map1);

        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);

    }

    @Test(expectedExceptions = AccountException.class)
    public void errorIfTwoPendingOrdersForSameMarket() {
        account.addPendingOrder(name1, Prediction.DOWN);
        account.addPendingOrder(name1, Prediction.DOWN);
    }

    @Test(expectedExceptions = AccountException.class)
    public void errorIfPreviousTradeNotClosed() {
        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);
        account.addPendingOrder(name1, Prediction.DOWN);
        account.openTrades(map1);
    }

    private void compare(BigDecimal one, BigDecimal two) {
        assertEquals(
                one.setScale(10, BigDecimal.ROUND_HALF_DOWN),
                two.setScale(10, BigDecimal.ROUND_HALF_DOWN)
        );
    }

    class MockProfitRecorder extends ProfitRecorder {

        boolean calledWinRate = false;
        int addTradeResultCount = 0;
        double profit = 0.0;

        MockProfitRecorder(UserOutput output) {
            super(output);
        }

        @Override
        void outputWinRateForAllRecords() {
            calledWinRate = true;
        }

        @Override
        void addTradeResult(DataSetName name, double profit) {
            addTradeResultCount++;
            this.profit += profit;
        }
    }

    class MockUserOutput extends CsvOutput {

        BigDecimal accountOpening;
        BigDecimal accountClosing;
        BigDecimal cashPerTrade;
        BigDecimal quantity;
        BigDecimal profit;
        BigDecimal closePrice;
        BigDecimal cash;

        Prediction prediction;
        DataSetName name;
        Double price;
        String message;

        MockUserOutput() throws IOException {
        }

        void clear() {
            accountOpening = null;
            accountClosing = null;
            cashPerTrade = null;
            quantity = null;
            profit = null;
            cash = null;
            prediction = null;
            name = null;
            price = Double.NaN;
            message = null;
        }

        @Override
        public void reportAccountOpening(BigDecimal cash) {
            accountOpening = cash;
        }

        @Override
        public void reportAccountClosing(BigDecimal cash) {
            accountClosing = cash;
        }


        @Override
        public void reportPendingTrade(DataSetName name, Prediction prediction) {
            this.name = name;
            this.prediction = prediction;
        }

        @Override
        public void reportOpeningTrade(DataSetName name, BigDecimal quantity, Double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        @Override
        public void reportClosingTrade(DataSetName name, BigDecimal quantity, BigDecimal closePrice, BigDecimal profit, BigDecimal cash) {
            this.name = name;
            this.quantity = quantity;
            this.closePrice = closePrice;
            this.profit = profit;
            this.cash = cash;
        }

    }
}
