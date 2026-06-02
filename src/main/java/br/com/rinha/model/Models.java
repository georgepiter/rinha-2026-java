package br.com.rinha.model;

import java.util.List;

public class Models {
    public static class TransactionRequest {
        public String id;
        public TxData transaction;
        public CustomerData customer;
        public MerchantData merchant;
        public TerminalData terminal;
        public LastTransaction last_transaction;
    }

    public static class TxData {
        public double amount;
        public int installments;
        public String requested_at;
    }

    public static class CustomerData {
        public double avg_amount;
        public int tx_count_24h;
        public List<String> known_merchants;
    }

    public static class MerchantData {
        public String id;
        public String mcc;
        public double avg_amount;
    }

    public static class TerminalData {
        public boolean is_online;
        public boolean card_present;
        public double km_from_home;
    }

    public static class LastTransaction {
        public String timestamp;
        public double km_from_current;
    }

    public static class Reference {
        public double[] vector;
        public String label;
    }
}
