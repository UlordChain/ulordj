/*
 * Copyright 2013 Google Inc.
 * Copyright 2018 Andreas Schildbach
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

package org.ulordj.params;

import org.ulordj.core.Block;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
public class RegTestParams extends AbstractUlordNetParams {
    private static final BigInteger MAX_TARGET = new BigInteger("0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f", 16);
    public static final int TESTNET_MAJORITY_WINDOW = 100;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51;

    public RegTestParams() {
        super(ID_REGTEST);
        packetMagic = 0xf0c5bbd0L;
        addressHeader = 140;
        p2shHeader = 120;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        targetTimespan = TARGET_TIMESPAN;
        dumpedPrivateKeyHeader = 239;
        segwitAddressHrp = "tb";
        genesisBlock.setTime(1526946000L);
        genesisBlock.setDifficultyTarget(Long.parseLong("200f0f0f", 16));
        genesisBlock.setNonce(new BigInteger("0000ec7bfb02cb74cc021bbc03773834a65f8a16655212b5abc8841efbea0000", 16));
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("0c1417be9488c5bc9c2974a8c2edf89089fdf9fa3e0e4a0d4c668ab973fcf8b4"));
        dnsSeeds = null;
        addrSeeds = null;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;
        bip44HeaderCoin = 0x80000001;   // Ulord BIP44 coin type is '247'

        // Difficulty adjustments are disabled for regtest.
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never
        // changes.
        interval = Integer.MAX_VALUE;
        maxTarget = MAX_TARGET;
        subsidyDecreaseBlockCount = 150;
        port = 29888;
        //id = ID_REGTEST;

        nPowMaxAdjustDown = 0;
        nPowMaxAdjustUp = 0;
        minActualTimespan = (averagingWindowTimespan * (100 - nPowMaxAdjustUp)) / 100;
        maxActualTimespan = (averagingWindowTimespan * (100 + nPowMaxAdjustDown)) / 100;

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return true;
    }

    private static Block genesis;

    @Override
    public Block getGenesisBlock() {
        synchronized (RegTestParams.class) {
            if (genesis == null) {
                genesis = super.getGenesisBlock();
                genesis.setNonce(new BigInteger("0000ec7bfb02cb74cc021bbc03773834a65f8a16655212b5abc8841efbea0000", 16));
                genesis.setDifficultyTarget(Long.parseLong("200f0f0f", 16));
                genesis.setTime(1526946000L);
                checkState(genesis.getHashAsString().toLowerCase().equals("0c1417be9488c5bc9c2974a8c2edf89089fdf9fa3e0e4a0d4c668ab973fcf8b4"));
            }
            return genesis;
        }
    }

    private static RegTestParams instance;
    public static synchronized RegTestParams get() {
        if (instance == null) {
            instance = new RegTestParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_REGTEST;
    }
}
