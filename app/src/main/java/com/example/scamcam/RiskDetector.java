package com.example.scamcam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RiskDetector {

    private static final HashMap<String, Integer> RISK_NUMBER = new HashMap<>();
    private static int threshold = 72;
    private static final List<String> currencies = Arrays.asList("$", "dollar", "euro", "pound", "dong", "rupee", "yen", "shilling", "coin", "gold", "money", "bucks");

    static {
        RISK_NUMBER.put("cryptocurrency", 5);
        RISK_NUMBER.put("bitcoin", 20);
        RISK_NUMBER.put("bank", 20);
        RISK_NUMBER.put("details", 15);
        RISK_NUMBER.put("credit", 15);
        RISK_NUMBER.put("password", 20);
        RISK_NUMBER.put("account", 10);
        RISK_NUMBER.put("transfer", 10);
        RISK_NUMBER.put("giftcard", 10);
        RISK_NUMBER.put("trust", 10);
        RISK_NUMBER.put("repay", 5);
        // below are common scamming words from online
        RISK_NUMBER.put("dhl", 10);
        RISK_NUMBER.put("notification", 10);
        RISK_NUMBER.put("delivery", 10);
        RISK_NUMBER.put("express", 10);
        RISK_NUMBER.put("label", 10);
        RISK_NUMBER.put("shipment", 10);
        RISK_NUMBER.put("ups", 10);
        RISK_NUMBER.put("international", 10);
        RISK_NUMBER.put("parcel", 10);
        RISK_NUMBER.put("alert", 10);
        RISK_NUMBER.put("urgent", 20);
        RISK_NUMBER.put("confirmation", 10);
        RISK_NUMBER.put("usps", 10);
        RISK_NUMBER.put("claim", 10);
        RISK_NUMBER.put("prize", 20);
        RISK_NUMBER.put("awards office", 10);
        RISK_NUMBER.put("guaranteed", 10);
        RISK_NUMBER.put("opportunity", 10);
        RISK_NUMBER.put("risk-free", 20);
        RISK_NUMBER.put("fortune", 15);
        RISK_NUMBER.put("rich", 10);
    }

    private int risk = 0;
    private int money_mentioned = 0;
    private final List<Integer> numbers_recorded = new ArrayList<>();

    RiskDetector() {

    }

    private static Integer sum(List<Integer> list) {
        int sum = 0;
        for (Integer num : list) {
            sum += num;
        }
        return sum;
    }

// --Commented out by Inspection START (2019-07-07 11:38):
//    public static Integer max(List<Integer> list) {
//        if (list == null || list.size() == 0)
//            return Integer.MIN_VALUE;
//        return Collections.max(list);
//    }
// --Commented out by Inspection STOP (2019-07-07 11:38)

    void setThreshold(int thresh) {
        threshold = thresh;
    }

    int getHighThreshold() {
        return threshold;
    }

    int getMediumThreshold() {
        return threshold / 2;
    }

    public void parseText(String text) {
        text = text.toLowerCase();

        for (String currency : currencies) {
            if (text.contains(currency)) {
                money_mentioned += 1;
            }
        }

        text = text.replaceAll(",", "");
        List<String> tmp = Arrays.asList(text.replaceAll("[^0-9]+", " ").trim().split(" "));
        if (tmp.size() != 0) {
            for (String s : tmp) {
                if (!s.isEmpty()) {
                    numbers_recorded.add(Integer.parseInt(s));
                }
            }
        }

        for (Map.Entry<String, Integer> entry : RISK_NUMBER.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (text.contains(key)) {
                risk += value;
            }
        }
    }

    public int getRiskValue() {
        int r = money_mentioned;
        if (money_mentioned > 0) {
            r += sum(numbers_recorded) / 10;
        }

        return Math.min(risk + r, 100);
    }

    public String getRisk() {
        int r = getRiskValue();
        return r >= getHighThreshold() ? "High Risk" : r >= getMediumThreshold() ? "Medium Risk" : "Low Risk";
    }
}
