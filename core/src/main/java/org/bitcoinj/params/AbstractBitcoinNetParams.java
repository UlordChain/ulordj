/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import org.bitcoinj.core.BitcoinSerializer;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractBitcoinNetParams extends NetworkParameters {

    /**
     * Scheme part for Bitcoin URIs.
     */
    public static final String BITCOIN_SCHEME = "ulord";
    //public static final int REWARD_HALVING_INTERVAL = 210000;

    private static final Logger log = LoggerFactory.getLogger(AbstractBitcoinNetParams.class);

    public AbstractBitcoinNetParams(String id) {
        super(id);
    }

    /**
     * Checks if we are at a reward halving point.
     * @param height The height of the previous stored block
     * @return If this is a reward halving point
     */
    //public final boolean isRewardHalvingPoint(final int height) {
    //    return ((height + 1) % REWARD_HALVING_INTERVAL) == 0;
    //}

    /**
     * Checks if we are at a difficulty transition point.
     * @param height The height of the previous stored block
     * @return If this is a difficulty transition point
     */
    public final boolean isDifficultyTransitionPoint(final int height) {
        return ((height + 1) % this.getInterval()) == 0;
    }

    @Override
    public void checkDifficultyTransitions(final StoredBlock storedPrev, final Block nextBlock,
    	final BlockStore blockStore) throws VerificationException, BlockStoreException {


//        // Return if the previous block is genesis
//        if(nextBlock.getHash().compareTo(this.genesisBlock.getHash()) == 0) {
//            return;
//        }

        Block prev = storedPrev.getHeader();

        // Find the first block in the averaging interval
        StoredBlock cursor = blockStore.get(prev.getHash());
        BigInteger nBitsTotal = BigInteger.ZERO;
        for(int i = 0; !cursor.getHeader().getHash().equals(this.genesisBlock.getHash())  && i < this.N_POW_AVERAGING_WINDOW; ++i) {
            //System.out.println("Diff " + cursor.getHeight() + " " + cursor.getHeader().getDifficultyTargetAsInteger().toString(16));
            Block currentBlock = cursor.getHeader();
            BigInteger nBitsTemp = currentBlock.getDifficultyTargetAsInteger();
            nBitsTotal = nBitsTotal.add(nBitsTemp);
            cursor = blockStore.get(currentBlock.getPrevBlockHash());
        }

        if(cursor.getHeader().getHash() == genesisBlock.getHash())
        {
            // Check if the difficulty didn't change
            if(nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Difficulty did not match");
            return;
        }

        // Find the average
        BigInteger nBitsAvg = nBitsTotal.divide(BigInteger.valueOf(this.N_POW_AVERAGING_WINDOW));
        System.out.println("Average: " + nBitsAvg.toString(16));
        //Block blockBeforePrevBlock = cursor.getHeader();

        //StoredBlock prevBlockOfPrevBlock = blockStore.get(prev.getPrevBlockHash());
        int timespan = (int) (prev.getTimeSeconds() - cursor.getHeader().getTimeSeconds());
        timespan = this.averagingWindowTimespan + (timespan - this.averagingWindowTimespan)/4;

        if(timespan < this.minActualTimespan)
            timespan = minActualTimespan;
        if(timespan > this.maxActualTimespan)
            timespan = maxActualTimespan;


        // create target with all the previous blocks / 17
        BigInteger expectedTarget = nBitsAvg;
        expectedTarget = expectedTarget.divide(BigInteger.valueOf(averagingWindowTimespan));
        expectedTarget = expectedTarget.multiply(BigInteger.valueOf(timespan));

        if(expectedTarget.compareTo(this.getMaxTarget()) > 0) {
            expectedTarget = this.getMaxTarget();
            log.info("Difficulty hit proof of work limit: {}", expectedTarget.toString(16));
        }

        long receivedTargetCompact = nextBlock.getDifficultyTarget();
        long expectedTargetCompact = Utils.encodeCompactBits(expectedTarget);

        if(expectedTargetCompact != receivedTargetCompact)
        {
            System.out.println("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(expectedTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(expectedTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
        }
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        return version.getBitcoinProtocolVersion();
    }

    @Override
    public BitcoinSerializer getSerializer(boolean parseRetain) {
        return new BitcoinSerializer(this, parseRetain);
    }

    @Override
    public String getUriScheme() {
        return BITCOIN_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }
}
