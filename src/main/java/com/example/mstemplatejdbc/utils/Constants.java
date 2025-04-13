package com.example.mstemplatejdbc.utils;

public class Constants {

    public static final String customerId = "customerId";

    public static final String iban = "iban";

    public static final String balance = "balance";

        public static class SqlConstants{
            public static final String retriveAccountsSql = """
             SELECT iban, customerId, balance,\s
                    created_at AS createdAt,\s
                    updated_at AS updatedAt\s
             FROM account_db\s
             WHERE customerId = :customerId
            \s""";
            public static final String insertSql = "INSERT INTO account_db (iban, customerId, balance)\n" +
                    "        VALUES (:iban, :customerId, :balance)";


            public static final String updateSql = "UPDATE account_db SET balance = :balance WHERE iban = :iban AND customerId = :customerId";

            public static final String deleteSql = "DELETE FROM account_db WHERE iban = :iban AND customerId = :customerId";
        }





}
