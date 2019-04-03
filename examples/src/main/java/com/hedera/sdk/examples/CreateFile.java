package com.hedera.sdk.examples;

import com.hedera.sdk.AccountId;
import com.hedera.sdk.Client;
import com.hedera.sdk.TransactionGetReceiptQuery;
import com.hedera.sdk.TransactionId;
import com.hedera.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.sdk.file.FileCreateTransaction;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@SuppressWarnings("Duplicates")
public final class CreateFile {
    public static void main(String[] args) throws InterruptedException {
        var env = Dotenv.load();

        var operatorKey = Ed25519PrivateKey.fromString(Objects.requireNonNull(env.get("OPERATOR_SECRET")));

        var client = new Client(env.get("NETWORK"));

        // The file is required to be a byte array,
        // you can easily use the bytes of a file instead.
        var fileContents = "Hedera hashgraph is great!".getBytes();

        var txId = new TransactionId(new AccountId(2));

        var tx = new FileCreateTransaction()
            .setTransactionId(txId)
            .setNodeAccount(new AccountId(3))
            .setExpirationTime(Instant.now().plus(Duration.ofSeconds(2592000)))
            .addKey(operatorKey.getPublicKey())
            .setContents(fileContents)
            .setMemo("[hedera-sdk-java][example] CreateFile")
            // The first signature represents the transaction payer
            .sign(operatorKey)
            // The second signature represents the owner of the file
            .sign(operatorKey);

        var res = tx.execute(client);

        System.out.println("transaction: " + res.toString());

        // Sleep for 4 seconds
        Thread.sleep(4000);

        var query = new TransactionGetReceiptQuery()
            .setTransaction(txId);

        var receipt = query.execute(client);
        var receiptStatus = receipt.getReceipt().getStatus();

        System.out.println("status: " + receiptStatus.toString());

        var newFileId = receipt.getReceipt().getFileID();

        System.out.println("new file num: " + newFileId.getFileNum());
    }
}
