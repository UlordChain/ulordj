/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.params;

import java.math.BigInteger;
import java.util.Date;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends AbstractBitcoinNetParams {
    public static final int TESTNET_MAJORITY_WINDOW = 100;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51;

    public TestNet3Params() {
        super(ID_TESTNET);
        //id = ID_TESTNET;
        packetMagic = 0xC2E6CEF3;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = new BigInteger("000fffffff000000000000000000000000000000000000000000000000000000", 16);
        port = 19888;           // Ulord Testnet port
        addressHeader = 130;    // Ulord Testnet addresses start with 'u' - PUBKEY_ADDRESS
        p2shHeader = 125;       // Ulord Testnet script address start with 's' - SCRIPT_ADDRESS
        dumpedPrivateKeyHeader = 239;
        segwitAddressHrp = "tb";
        genesisBlock.setTime(1524057440L);
        genesisBlock.setDifficultyTarget(521142271L);
        genesisBlock.setNonce(new BigInteger("000020f00dd1af082323e02e1f5b1d866d777abbcf63ba720d35dcf585840073", 16));
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 840960;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000f378be841f44e75346eebd931b13041f0dee561af6a80cfea6669c1bfec03"));
        alertSigningKey = Utils.HEX.decode("04302390343f91cc401d56d68b123028bf52e5fca1939df127f63c6467cdf9c8e2c14b61104cf817d0b780da337893ecc4aaff1309e536162dabbdb45200ca2b0a");

        minActualTimespan = averagingWindowTimespan * (100 - nPowMaxAdjustUp)/100;
        maxActualTimespan = averagingWindowTimespan * (100 + nPowMaxAdjustDown)/100;

        dnsSeeds = new String[] {
                "node.ulord.one",
                "10.221.153.180",
                "119.27.188.44"
        };
        addrSeeds = null;
        bip32HeaderPub = 0x043587CF;    // Ulord BIP32 pubkeys start with 'xpub' (Bitcoin defaults)
        bip32HeaderPriv = 0x04358394;   // Ulord BIP32 prvkeys start with 'xprv' (Bitcoin defaults)
        bip44HeaderCoin = 0x80000001;   // Ulord BIP44 coin type is '247'

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;
    }

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }

    // February 16th 2012
    private static final Date testnetDiffDate = new Date(1329264000000L);

//    @Override
//    public void checkDifficultyTransitions(final StoredBlock storedPrev, final Block nextBlock,
//        final BlockStore blockStore) throws VerificationException, BlockStoreException {
//
//        Block prev = storedPrev.getHeader();
//
//        if(nextBlock.getTime().after(prev.getTime())) {
//            if(storedPrev.getHeight() < 18) {
//                // Check if for first 19 block if the difficulty didn't change
//                if(nextBlock.getDifficultyTarget() !=  prev.getDifficultyTarget()) {
//                    System.out.println("Unexpected change in difficulty at height " + storedPrev.getHeight() +
//                            ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
//                            Long.toHexString(prev.getDifficultyTarget()));
//                    throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
//                            ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
//                            Long.toHexString(prev.getDifficultyTarget()));
//                }
//            }
//            else {
//                super.checkDifficultyTransitions(storedPrev, nextBlock, blockStore);
//            }
//        }
//        else {
//            System.out.println("Next block time cannot be before previous block time. " +
//                    nextBlock.getTimeSeconds() + " vs " + prev.getTimeSeconds());
//            throw new VerificationException("Next block time cannot be before previous block time. " +
//                    nextBlock.getTimeSeconds() + " vs " + prev.getTimeSeconds());
//        }
//
//    }
}
