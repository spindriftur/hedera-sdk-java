package com.hedera.hashgraph.sdk.account;

import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hederahashgraph.api.proto.java.CryptoGetInfoResponse;
import com.hederahashgraph.api.proto.java.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountInfoTest {
    private static final Ed25519PrivateKey privateKey = Ed25519PrivateKey.generate();

    @Test
    @DisplayName("won't deserialize from the wrong kind of response")
    void incorrectResponse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AccountInfo(Response.getDefaultInstance())
        );
    }

    @Test
    @DisplayName("requires a key")
    void requiresKey() {
        final Response response = Response.newBuilder()
            .setCryptoGetInfo(CryptoGetInfoResponse.getDefaultInstance())
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> new AccountInfo(response),
            "query response missing key"
        );
    }

    @Test
    @DisplayName("deserializes from a correct response")
    void correct() {
        final Response response = Response.newBuilder()
            .setCryptoGetInfo(
                CryptoGetInfoResponse.newBuilder()
                    .setAccountInfo(CryptoGetInfoResponse.AccountInfo.newBuilder()
                        .setKey(privateKey.getPublicKey().toKeyProto())))
            .build();

        final AccountInfo accountInfo = new AccountInfo(response);

        assertEquals(accountInfo.getAccountId(), new AccountId(0));
        assertEquals(accountInfo.getContractAccountId(), "");
        assertNull(accountInfo.getProxyAccountId());
        assertEquals(accountInfo.getClaims(), new ArrayList<>());
    }

}