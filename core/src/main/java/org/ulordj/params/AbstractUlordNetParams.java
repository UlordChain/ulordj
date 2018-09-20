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

package org.ulordj.params;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.Arrays;

import org.ulordj.core.*;
import org.ulordj.utils.MonetaryFormat;
import org.ulordj.store.BlockStore;
import org.ulordj.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractUlordNetParams extends NetworkParameters {

    /**
     * Scheme part for Bitcoin URIs.
     */
    public static final String BITCOIN_SCHEME = "ulord";
    //public static final int REWARD_HALVING_INTERVAL = 210000;

    private static final Logger log = LoggerFactory.getLogger(AbstractUlordNetParams.class);

    public AbstractUlordNetParams(String id) {
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

        if((storedPrev.getHeight() + 1) % N_POW_AVERAGING_WINDOW != 0) {
            return;
        }

        Block prev = storedPrev.getHeader();
        // Find the first block in the averaging interval
        StoredBlock cursor = blockStore.get(nextBlock.getPrevBlockHash());
        BigInteger nBitsTotal = BigInteger.ZERO;
        for(int i = 0; !cursor.getHeader().getHash().equals(this.genesisBlock.getHash())  && i < this.N_POW_AVERAGING_WINDOW; ++i) {
            nBitsTotal = nBitsTotal.add(cursor.getHeader().getDifficultyTargetAsInteger());
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }

        if(cursor == null)
            return;

        // Find the average
        BigInteger nBitsAvg = nBitsTotal.divide(BigInteger.valueOf(this.N_POW_AVERAGING_WINDOW));

        long prevBlockTimeSpan = getMedianTimestampOfRecentBlocks(storedPrev, blockStore);
        long firstBlockTimeSpan = getMedianTimestampOfRecentBlocks(cursor, blockStore);
        long timespan = (prevBlockTimeSpan - firstBlockTimeSpan);//cursor.getHeader().getTimeSeconds());
        timespan = this.averagingWindowTimespan + (timespan - this.averagingWindowTimespan) / 4;

        if(timespan < this.minActualTimespan)
            timespan = minActualTimespan;
        if(timespan > this.maxActualTimespan)
            timespan = maxActualTimespan;


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

    /**
     * Gets the median timestamp of the last 11 blocks
     */
    private long getMedianTimestampOfRecentBlocks(StoredBlock storedBlock,
                                                         BlockStore store) throws BlockStoreException {
        long[] timestamps = new long[11];
        int unused = 9;
        timestamps[10] = storedBlock.getHeader().getTimeSeconds();
        while (unused >= 0 && (storedBlock = storedBlock.getPrev(store)).getHeader().getHash() != this.genesisBlock.getHash())
            timestamps[unused--] = storedBlock.getHeader().getTimeSeconds();

        Arrays.sort(timestamps, unused+1, 11);
        return timestamps[unused + (11-unused)/2];
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
    public UlordSerializer getSerializer(boolean parseRetain) {
        return new UlordSerializer(this, parseRetain);
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
