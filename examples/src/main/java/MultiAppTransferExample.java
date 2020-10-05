import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.CryptoTransferTransaction;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaPreCheckStatusException;
import com.hedera.hashgraph.sdk.HederaReceiptStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.sdk.Transaction;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class MultiAppTransferExample {
    // see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
    // the exchange should possess this key, we're only generating it for demonstration purposes
    private static final PrivateKey exchangeKey = PrivateKey.generate();
    // this is the only key we should actually possess
    private static final PrivateKey userKey = PrivateKey.generate();
    private static final Client client;

    static {
        client = Client.forTestnet();

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);
    }

    private MultiAppTransferExample() {
    }

    public static void main(String[] args) throws HederaReceiptStatusException, TimeoutException, HederaPreCheckStatusException {
        // the exchange creates an account for the user to transfer funds to
        AccountId exchangeAccountId = new AccountCreateTransaction()
            // the exchange only accepts transfers that it validates through a side channel (e.g. REST API)
            .setReceiverSignatureRequired(true)
            .setKey(exchangeKey)
            // The owner key has to sign this transaction
            // when setReceiverSignatureRequired is true
            .freezeWith(client)
            .sign(exchangeKey)
            .execute(client)
            .getReceipt(client)
            .accountId;

        assert exchangeAccountId != null;

        // for the purpose of this example we create an account for
        // the user with a balance of 5 h
        AccountId userAccountId = new AccountCreateTransaction()
            .setInitialBalance(new Hbar(5))
            .setKey(userKey)
            .execute(client)
            .getReceipt(client)
            .accountId;

        assert userAccountId != null;

        // next we make a transfer from the user account to the
        // exchange account, this requires signing by both parties
        CryptoTransferTransaction transferTxn = new CryptoTransferTransaction()
            .addRecipient(exchangeAccountId, new Hbar(2))
            .addSender(userAccountId, new Hbar(2))
            // the exchange-provided memo required to validate the transaction
            .setTransactionMemo("https://some-exchange.com/user1/account1")
            // NOTE: to manually sign, you must freeze the Transaction first
            .freezeWith(client)
            .sign(userKey);

        // the exchange must sign the transaction in order for it to be accepted by the network
        // assume this is some REST call to the exchange API server
        byte[] signedTxnBytes = exchangeSignsTransaction(transferTxn.toBytes());

        // parse the transaction bytes returned from the exchange
        Transaction<?> signedTransferTxn = Transaction.fromBytes(signedTxnBytes);

        // get the amount we are about to transfer
        // we built this with +2, -2
        Hbar transferAmount = ((CryptoTransferTransaction)signedTransferTxn).getTransfers().get(0).amount;

        System.out.println("about to transfer " + transferAmount + "...");

        // we now execute the signed transaction and wait for it to be accepted
        TransactionResponse transactionResponse = signedTransferTxn.execute(client);

        // (important!) wait for consensus by querying for the receipt
        transactionResponse.getReceipt(client);

        Hbar senderBalanceAfter = new AccountBalanceQuery()
            .setAccountId(userAccountId)
            .execute(client);

        Hbar receiptBalanceAfter = new AccountBalanceQuery()
            .setAccountId(exchangeAccountId)
            .execute(client);

        System.out.println("" + userAccountId + " balance = " + senderBalanceAfter);
        System.out.println("" + exchangeAccountId + " balance = " + receiptBalanceAfter);
    }

    private static byte[] exchangeSignsTransaction(byte[] transactionData) {
        return Transaction.fromBytes(transactionData).sign(exchangeKey).toBytes();
    }
}