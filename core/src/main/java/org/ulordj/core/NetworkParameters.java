/*
 * Copyright 2011 Google Inc.
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

package org.ulordj.core;

import com.google.common.base.Objects;
import org.ulordj.net.discovery.*;
import org.ulordj.params.*;
import org.ulordj.script.*;
import org.ulordj.store.BlockStore;
import org.ulordj.store.BlockStoreException;

import org.ulordj.utils.MonetaryFormat;

import javax.annotation.*;
import java.io.*;
import java.math.*;
import java.util.*;

import static org.ulordj.core.Coin.*;
import org.ulordj.utils.VersionTally;

/**
 * <p>NetworkParameters contains the data needed for working with an instantiation of a Bitcoin chain.</p>
 *
 * <p>This is an abstract class, concrete instantiations can be found in the params package. There are four:
 * one for the main network ({@link MainNetParams}), one for the public test network, and two others that are
 * intended for unit testing and local app development purposes. Although this class contains some aliases for
 * them, you are encouraged to call the static get() methods on each specific params class directly.</p>
 */
public abstract class NetworkParameters {
    /**
     * The alert signing key originally owned by Satoshi, and now passed on to Gavin along with a few others.
     */
    public static final byte[] SATOSHI_KEY = Utils.HEX.decode("04fc9702847840aaf195de8442ebecedf5b095cdbb9bc716bda9110971b28a49e0ead8564ff0db22209e0374782c093bb899692d524e9d6a6956e7c5ecbcd68284");

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_MAINNET = "org.ulord.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_TESTNET = "org.ulord.test";
    /** The string returned by getId() for regtest mode. */
    public static final String ID_REGTEST = "org.ulord.regtest";
    /** Unit test network. */
    public static final String ID_UNITTESTNET = "co.usc.ulordj.unittest";

    /** The string used by the payment protocol to represent the main net. */
    public static final String PAYMENT_PROTOCOL_ID_MAINNET = "main";
    /** The string used by the payment protocol to represent the test net. */
    public static final String PAYMENT_PROTOCOL_ID_TESTNET = "test";
    /** The string used by the payment protocol to represent unit testing (note that this is non-standard). */
    public static final String PAYMENT_PROTOCOL_ID_UNIT_TESTS = "unittest";
    public static final String PAYMENT_PROTOCOL_ID_REGTEST = "regtest";

    // TODO: Seed nodes should be here as well.

    protected Block genesisBlock;
    protected BigInteger maxTarget;
    protected int port;
    protected long packetMagic;  // Indicates message origin network and is used to seek to the next message when stream state is unknown.
    protected int addressHeader;
    protected int p2shHeader;
    protected int dumpedPrivateKeyHeader;
    protected String segwitAddressHrp;
    protected int interval;
    protected int targetTimespan;
    protected byte[] alertSigningKey;
    protected int bip32HeaderPub;
    protected int bip32HeaderPriv;
    protected int bip44HeaderCoin;

    /** Used to check majorities for block version upgrade */
    protected int majorityEnforceBlockUpgrade;
    protected int majorityRejectBlockOutdated;
    protected int majorityWindow;

    /**
     * See getId(). This may be null for old deserialized wallets. In that case we derive it heuristically
     * by looking at the port number.
     */
    protected String id;

    /**
     * The depth of blocks required for a coinbase transaction to be spendable.
     */
    protected int spendableCoinbaseDepth;
    protected int subsidyDecreaseBlockCount;

    protected int[] acceptableAddressCodes;
    protected String[] dnsSeeds;
    protected int[] addrSeeds;
    protected HttpDiscovery.Details[] httpSeeds = {};
    protected Map<Integer, Sha256Hash> checkpoints = new HashMap<>();
    protected transient MessageSerializer defaultSerializer = null;

    protected NetworkParameters(String id) {
        alertSigningKey = SATOSHI_KEY;
        this.id = id;
        genesisBlock = createGenesis(this);
    }

    private static Block createGenesis(NetworkParameters n) {
        Block genesisBlock = new Block(n, Block.BLOCK_VERSION_GENESIS);
        Transaction t = new Transaction(n);
        ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
        try {
            // A script containing the difficulty bits and the following message:
            //
            // "Change the World with Us. 22/May/2018, 00:00:00, GMT"
            if(n.id == ID_MAINNET || n.id == ID_REGTEST) {
                byte[] bytes = Utils.HEX.decode
                        ("04ffff001d0104344368616e67652074686520576f726c6420776974682055732e2032322f4d61792f323031382c2030303a30303a30302c20474d54");
                t.addInput(new TransactionInput(n, t, bytes));
                Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                        ("041c508f27e982c369486c0f1a42779208b3f5dc96c21a2af6004cb18d1529f42182425db1e1632dc6e73ff687592e148569022cee52b4b4eb10e8bb11bd927ec0"));
            }else{
                // A script containing the difficulty bits and the following message:
                //
                // "ulord hold value testnet."
                byte[] bytes = Utils.HEX.decode
                        ("04ffff001d010419756c6f726420686f6c642076616c756520746573746e65742e");
                t.addInput(new TransactionInput(n, t, bytes));
                Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                        ("034c73d75f59061a08032b68369e5034390abc5215b3df79be01fb4319173a88f8"));
            }
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(n, t, ONE_COIN, scriptPubKeyBytes.toByteArray()));
            genesisBlock.addTransaction(t);
            return genesisBlock;
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
    }

    public static final int TARGET_TIMESPAN = 24 * 60 * 60;  // 1 day per difficulty cycle, on average.
    public static final int TARGET_SPACING = (int)(2.5 * 60);  // 2.5 minutes per block.
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;

    /** Used to validate and update block's difficulty*/
    protected final int N_POW_AVERAGING_WINDOW = 17;                              // nPowAveragingWindow = 17 chainparams.cpp
    protected int averagingWindowTimespan = N_POW_AVERAGING_WINDOW * TARGET_SPACING;
    protected int nPowMaxAdjustDown;
    protected int nPowMaxAdjustUp;
    protected int minActualTimespan;
    protected int maxActualTimespan;

    /**
     * Blocks with a timestamp after this should enforce BIP 16, aka "Pay to script hash". This BIP changed the
     * network rules in a soft-forking manner, that is, blocks that don't follow the rules are accepted but not
     * mined upon and thus will be quickly re-orged out as long as the majority are enforcing the rule.
     */
    public static final int BIP16_ENFORCE_TIME = 1333238400;
    
    /**
     * The maximum number of coins to be generated
     */
    public static final long MAX_COINS = 1000000000;

    /**
     * The maximum money to be generated
     */
    public static final Coin MAX_MONEY = COIN.multiply(MAX_COINS);

    /** Alias for TestNet3Params.get(), use that instead. */
    @Deprecated
    public static NetworkParameters testNet() {
        return TestNet3Params.get();
    }

    /** Alias for TestNet3Params.get(), use that instead. */
    @Deprecated
    public static NetworkParameters testNet3() {
        return TestNet3Params.get();
    }

    /** Alias for MainNetParams.get(), use that instead */
    @Deprecated
    public static NetworkParameters prodNet() {
        return MainNetParams.get();
    }

    /** Returns a testnet params modified to allow any difficulty target. */
    @Deprecated
    public static NetworkParameters unitTests() {
        return UnitTestParams.get();
    }

    /** Returns a standard regression test params (similar to unitTests) */
    @Deprecated
    public static NetworkParameters regTests() {
        return RegTestParams.get();
    }

    /**
     * A Java package style string acting as unique ID for these parameters
     */
    public String getId() {
        return id;
    }

    public abstract String getPaymentProtocolId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getId().equals(((NetworkParameters)o).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    /** Returns the network parameters for the given string ID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromID(String id) {
        if (id.equals(ID_MAINNET)) {
            return MainNetParams.get();
        } else if (id.equals(ID_TESTNET)) {
            return TestNet3Params.get();
        } else if (id.equals(ID_UNITTESTNET)) {
            return UnitTestParams.get();
        } else if (id.equals(ID_REGTEST)) {
            return RegTestParams.get();
        } else {
            return null;
        }
    }

    /** Returns the network parameters for the given string paymentProtocolID or NULL if not recognized. */
    @Nullable
    public static NetworkParameters fromPmtProtocolID(String pmtProtocolId) {
        if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_MAINNET)) {
            return MainNetParams.get();
        } else if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_TESTNET)) {
            return TestNet3Params.get();
        } else if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_UNIT_TESTS)) {
            return UnitTestParams.get();
        } else if (pmtProtocolId.equals(PAYMENT_PROTOCOL_ID_REGTEST)) {
            return RegTestParams.get();
        } else {
            return null;
        }
    }

    public int getSpendableCoinbaseDepth() {
        return spendableCoinbaseDepth;
    }

    /**
     * Throws an exception if the block's difficulty is not correct.
     *
     * @throws VerificationException if the block's difficulty is not correct.
     */
    public abstract void checkDifficultyTransitions(StoredBlock storedPrev, Block next, final BlockStore blockStore) throws VerificationException, BlockStoreException;

    /**
     * Returns true if the block height is either not a checkpoint, or is a checkpoint and the hash matches.
     */
    public boolean passesCheckpoint(int height, Sha256Hash hash) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash == null || checkpointHash.equals(hash);
    }

    /**
     * Returns true if the given height has a recorded checkpoint.
     */
    public boolean isCheckpoint(int height) {
        Sha256Hash checkpointHash = checkpoints.get(height);
        return checkpointHash != null;
    }

    public int getSubsidyDecreaseBlockCount() {
        return subsidyDecreaseBlockCount;
    }

    /** Returns DNS names that when resolved, give IP addresses of active peers. */
    public String[] getDnsSeeds() {
        return dnsSeeds;
    }

    /** Returns IP address of active peers. */
    public int[] getAddrSeeds() {
        return addrSeeds;
    }

    /** Returns discovery objects for seeds implementing the Cartographer protocol. See {@link HttpDiscovery} for more info. */
    public HttpDiscovery.Details[] getHttpSeeds() {
        return httpSeeds;
    }

    /**
     * <p>Genesis block for this chain.</p>
     *
     * <p>The first block in every chain is a well known constant shared between all Bitcoin implemenetations. For a
     * block to be valid, it must be eventually possible to work backwards to the genesis block by following the
     * prevBlockHash pointers in the block headers.</p>
     *
     * <p>The genesis blocks for both test and main networks contain the timestamp of when they were created,
     * and a message in the coinbase transaction. It says, <i>"The Times 03/Jan/2009 Chancellor on brink of second
     * bailout for banks"</i>.</p>
     */
    public Block getGenesisBlock() {
        return genesisBlock;
    }

    /** Default TCP port on which to connect to nodes. */
    public int getPort() {
        return port;
    }

    /** The header bytes that identify the start of a packet on this network. */
    public long getPacketMagic() {
        return packetMagic;
    }

    /**
     * First byte of a base58 encoded address. See {@link LegacyAddress}. This is the same as acceptableAddressCodes[0] and
     * is the one used for "normal" addresses. Other types of address may be encountered with version codes found in
     * the acceptableAddressCodes array.
     */
    public int getAddressHeader() {
        return addressHeader;
    }

    /**
     * First byte of a base58 encoded P2SH address.  P2SH addresses are defined as part of BIP0013.
     */
    public int getP2SHHeader() {
        return p2shHeader;
    }

    /** First byte of a base58 encoded dumped private key. See {@link DumpedPrivateKey}. */
    public int getDumpedPrivateKeyHeader() {
        return dumpedPrivateKeyHeader;
    }

    /** Human readable part of bech32 encoded segwit address. */
    public String getSegwitAddressHrp() {
        return segwitAddressHrp;
    }

    /**
     * How much time in seconds is supposed to pass between "interval" blocks. If the actual elapsed time is
     * significantly different from this value, the network difficulty formula will produce a different value. Both
     * test and main Bitcoin networks use 2 weeks (1209600 seconds).
     */
    public int getTargetTimespan() {
        return targetTimespan;
    }

    /**
     * If we are running in testnet-in-a-box mode, we allow connections to nodes with 0 non-genesis blocks.
     */
    public boolean allowEmptyPeerChain() {
        return true;
    }

    /** How many blocks pass between difficulty adjustment periods. Bitcoin standardises this to be 2016. */
    public int getInterval() {
        return interval;
    }

    /** Maximum target represents the easiest allowable proof of work. */
    public BigInteger getMaxTarget() {
        return maxTarget;
    }

    /**
     * The key used to sign {@link AlertMessage}s. You can use {@link ECKey#verify(byte[], byte[], byte[])} to verify
     * signatures using it.
     */
    public byte[] getAlertSigningKey() {
        return alertSigningKey;
    }

    /** Returns the 4 byte header for BIP32 (HD) wallet - public key part. */
    public int getBip32HeaderPub() {
        return bip32HeaderPub;
    }

    /** Returns the 4 byte header for BIP32 (HD) wallet - private key part. */
    public int getBip32HeaderPriv() {
        return bip32HeaderPriv;
    }

    /**
     * Returns the number of coins that will be produced in total, on this
     * network. Where not applicable, a very large number of coins is returned
     * instead (i.e. the main coin issue for Dogecoin).
     */
    public abstract Coin getMaxMoney();

    /**
     * Any standard (ie P2PKH) output smaller than this value will
     * most likely be rejected by the network.
     */
    public abstract Coin getMinNonDustOutput();

    /**
     * The monetary object for this currency.
     */
    public abstract MonetaryFormat getMonetaryFormat();

    /**
     * Scheme part for URIs, for example "ulord".
     */
    public abstract String getUriScheme();

    /**
     * Returns whether this network has a maximum number of coins (finite supply) or
     * not. Always returns true for Bitcoin, but exists to be overriden for other
     * networks.
     */
    public abstract boolean hasMaxMoney();

    /**
     * Return the default serializer for this network. This is a shared serializer.
     * @return the default serializer for this network.
     */
    public final MessageSerializer getDefaultSerializer() {
        // Construct a default serializer if we don't have one
        if (null == this.defaultSerializer) {
            // Don't grab a lock unless we absolutely need it
            synchronized(this) {
                // Now we have a lock, double check there's still no serializer
                // and create one if so.
                if (null == this.defaultSerializer) {
                    // As the serializers are intended to be immutable, creating
                    // two due to a race condition should not be a problem, however
                    // to be safe we ensure only one exists for each network.
                    this.defaultSerializer = getSerializer(false);
                }
            }
        }
        return defaultSerializer;
    }

    /**
     * Construct and return a custom serializer.
     */
    public abstract UlordSerializer getSerializer(boolean parseRetain);

    /**
     * The number of blocks in the last {@link #getMajorityWindow()} blocks
     * at which to trigger a notice to the user to upgrade their client, where
     * the client does not understand those blocks.
     */
    public int getMajorityEnforceBlockUpgrade() {
        return majorityEnforceBlockUpgrade;
    }

    /**
     * The number of blocks in the last {@link #getMajorityWindow()} blocks
     * at which to enforce the requirement that all new blocks are of the
     * newer type (i.e. outdated blocks are rejected).
     */
    public int getMajorityRejectBlockOutdated() {
        return majorityRejectBlockOutdated;
    }

    /**
     * The sampling window from which the version numbers of blocks are taken
     * in order to determine if a new block version is now the majority.
     */
    public int getMajorityWindow() {
        return majorityWindow;
    }

    /**
     * The flags indicating which block validation tests should be applied to
     * the given block. Enables support for alternative blockchains which enable
     * tests based on different criteria.
     * 
     * @param block block to determine flags for.
     * @param height height of the block, if known, null otherwise. Returned
     * tests should be a safe subset if block height is unknown.
     */
    public EnumSet<Block.VerifyFlag> getBlockVerificationFlags(final Block block,
            final VersionTally tally, final Integer height) {
        final EnumSet<Block.VerifyFlag> flags = EnumSet.noneOf(Block.VerifyFlag.class);

        if (block.isBIP34()) {
            final Integer count = tally.getCountAtOrAbove(Block.BLOCK_VERSION_BIP34);
            if (null != count && count >= getMajorityEnforceBlockUpgrade()) {
                flags.add(Block.VerifyFlag.HEIGHT_IN_COINBASE);
            }
        }
        return flags;
    }

    /**
     * The flags indicating which script validation tests should be applied to
     * the given transaction. Enables support for alternative blockchains which enable
     * tests based on different criteria.
     *
     * @param block block the transaction belongs to.
     * @param transaction to determine flags for.
     * @param height height of the block, if known, null otherwise. Returned
     * tests should be a safe subset if block height is unknown.
     */
    public EnumSet<Script.VerifyFlag> getTransactionVerificationFlags(final Block block,
            final Transaction transaction, final VersionTally tally, final Integer height) {
        final EnumSet<Script.VerifyFlag> verifyFlags = EnumSet.noneOf(Script.VerifyFlag.class);
        if (block.getTimeSeconds() >= NetworkParameters.BIP16_ENFORCE_TIME)
            verifyFlags.add(Script.VerifyFlag.P2SH);

        // Start enforcing CHECKLOCKTIMEVERIFY, (BIP65) for block.nVersion=4
        // blocks, when 75% of the network has upgraded:
        if (block.getVersion() >= Block.BLOCK_VERSION_BIP65 &&
            tally.getCountAtOrAbove(Block.BLOCK_VERSION_BIP65) > this.getMajorityEnforceBlockUpgrade()) {
            verifyFlags.add(Script.VerifyFlag.CHECKLOCKTIMEVERIFY);
        }

        return verifyFlags;
    }

    public abstract int getProtocolVersionNum(final ProtocolVersion version);

    public static enum ProtocolVersion {
        MINIMUM(70103),
        PONG(60001),
        BLOOM_FILTER(70201),
        WITNESS_VERSION(70103),
        CURRENT(70206);

        private final int ulordProtocol;

        ProtocolVersion(final int ulordProtocol) {
            this.ulordProtocol = ulordProtocol;
        }

        public int getBitcoinProtocolVersion() {
            return ulordProtocol;
        }
    }
}
