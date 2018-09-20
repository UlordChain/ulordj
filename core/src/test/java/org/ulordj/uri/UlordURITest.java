/*
 * Copyright 2012, 2014 the original author or authors.
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

package org.ulordj.uri;

import org.ulordj.core.Address;
import org.ulordj.core.LegacyAddress;
import org.ulordj.params.MainNetParams;
import org.ulordj.params.TestNet3Params;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.ulordj.core.Coin.*;
import org.ulordj.core.NetworkParameters;
import org.ulordj.core.SegwitAddress;

import static org.junit.Assert.*;

public class UlordURITest {
    private UlordURI testObject = null;

    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final String MAINNET_GOOD_ADDRESS = "1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH";
    private static final String MAINNET_GOOD_SEGWIT_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";
    private static final String BITCOIN_SCHEME = MAINNET.getUriScheme();

    @Test
    public void testConvertToBitcoinURI() throws Exception {
        Address goodAddress = LegacyAddress.fromBase58(MAINNET, MAINNET_GOOD_ADDRESS);
        
        // simple example
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello&message=AMessage", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("12.34"), "Hello", "AMessage"));
        
        // example with spaces, ampersand and plus
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello%20World&message=Mess%20%26%20age%20%2B%20hope", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("12.34"), "Hello World", "Mess & age + hope"));

        // no amount, label present, message present
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?label=Hello&message=glory", UlordURI.convertToBitcoinURI(goodAddress, null, "Hello", "glory"));
        
        // amount present, no label, message present
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("0.1"), null, "glory"));
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("0.1"), "", "glory"));

        // amount present, label present, no message
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("12.34"), "Hello", null));
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("12.34"), "Hello", ""));
              
        // amount present, no label, no message
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=1000", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("1000"), null, null));
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?amount=1000", UlordURI.convertToBitcoinURI(goodAddress, parseCoin("1000"), "", ""));
        
        // no amount, label present, no message
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?label=Hello", UlordURI.convertToBitcoinURI(goodAddress, null, "Hello", null));
        
        // no amount, no label, message present
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", UlordURI.convertToBitcoinURI(goodAddress, null, null, "Agatha"));
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", UlordURI.convertToBitcoinURI(goodAddress, null, "", "Agatha"));
      
        // no amount, no label, no message
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS, UlordURI.convertToBitcoinURI(goodAddress, null, null, null));
        assertEquals("ulord:" + MAINNET_GOOD_ADDRESS, UlordURI.convertToBitcoinURI(goodAddress, null, "", ""));

        // different scheme
        final NetworkParameters alternativeParameters = new MainNetParams() {
            @Override
            public String getUriScheme() {
                return "test";
            }
        };

        assertEquals("test:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello&message=AMessage",
             UlordURI.convertToBitcoinURI(LegacyAddress.fromBase58(alternativeParameters, MAINNET_GOOD_ADDRESS), parseCoin("12.34"), "Hello", "AMessage"));
    }

    @Test
    public void testConvertToBitcoinURI_segwit() throws Exception {
        assertEquals("ulord:" + MAINNET_GOOD_SEGWIT_ADDRESS + "?message=segwit%20rules", UlordURI.convertToBitcoinURI(
                SegwitAddress.fromBech32(MAINNET, MAINNET_GOOD_SEGWIT_ADDRESS), null, null, "segwit rules"));
    }

    @Test
    public void testGood_legacy() throws UlordURIParseException {
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS);
        assertEquals(MAINNET_GOOD_ADDRESS, testObject.getAddress().toString());
        assertNull("Unexpected amount", testObject.getAmount());
        assertNull("Unexpected label", testObject.getLabel());
        assertEquals("Unexpected label", 20, testObject.getAddress().getHash().length);
    }

    @Test
    public void testGood_segwit() throws UlordURIParseException {
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_SEGWIT_ADDRESS);
        assertEquals(MAINNET_GOOD_SEGWIT_ADDRESS, testObject.getAddress().toString());
        assertNull("Unexpected amount", testObject.getAmount());
        assertNull("Unexpected label", testObject.getLabel());
    }

    /**
     * Test a broken URI (bad scheme)
     */
    @Test
    public void testBad_Scheme() {
        try {
            testObject = new UlordURI(MAINNET, "blimpcoin:" + MAINNET_GOOD_ADDRESS);
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad syntax)
     */
    @Test
    public void testBad_BadSyntax() {
        // Various illegal characters
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + "|" + MAINNET_GOOD_ADDRESS);
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "\\");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        // Separator without field
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }
    }

    /**
     * Test a broken URI (missing address)
     */
    @Test
    public void testBad_Address() {
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME);
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad address type)
     */
    @Test
    public void testBad_IncorrectAddressType() {
        try {
            testObject = new UlordURI(TESTNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS);
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("Bad address"));
        }
    }

    /**
     * Handles a simple amount
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Amount() throws UlordURIParseException {
        // Test the decimal parsing
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210.12345678");
        assertEquals("654321012345678", testObject.getAmount().toString());

        // Test the decimal parsing
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=.12345678");
        assertEquals("12345678", testObject.getAmount().toString());

        // Test the integer parsing
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210");
        assertEquals("654321000000000", testObject.getAmount().toString());
    }

    /**
     * Handles a simple label
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Label() throws UlordURIParseException {
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=Hello%20World");
        assertEquals("Hello World", testObject.getLabel());
    }

    /**
     * Handles a simple label with an embedded ampersand and plus
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_LabelWithAmpersandAndPlus() throws UlordURIParseException {
        String testString = "Hello Earth & Mars + Venus";
        String encodedLabel = UlordURI.encodeURLString(testString);
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(testString, testObject.getLabel());
    }

    /**
     * Handles a Russian label (Unicode test)
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_LabelWithRussian() throws UlordURIParseException {
        // Moscow in Russian in Cyrillic
        String moscowString = "\u041c\u043e\u0441\u043a\u0432\u0430";
        String encodedLabel = UlordURI.encodeURLString(moscowString);
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(moscowString, testObject.getLabel());
    }

    /**
     * Handles a simple message
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Message() throws UlordURIParseException {
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=Hello%20World");
        assertEquals("Hello World", testObject.getMessage());
    }

    /**
     * Handles various well-formed combinations
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Combinations() throws UlordURIParseException {
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210&label=Hello%20World&message=Be%20well");
        assertEquals(
                "UlordURI['amount'='654321000000000','label'='Hello World','message'='Be well','address'='1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH']",
                testObject.toString());
    }

    /**
     * Handles a badly formatted amount field
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Amount() throws UlordURIParseException {
        // Missing
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }

        // Non-decimal (BIP 21)
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=12X4");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }
    }

    @Test
    public void testEmpty_Label() throws UlordURIParseException {
        assertNull(new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=").getLabel());
    }

    @Test
    public void testEmpty_Message() throws UlordURIParseException {
        assertNull(new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=").getMessage());
    }

    /**
     * Handles duplicated fields (sneaky address overwrite attack)
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Duplicated() throws UlordURIParseException {
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?address=aardvark");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("address"));
        }
    }

    @Test
    public void testGood_ManyEquals() throws UlordURIParseException {
        assertEquals("aardvark=zebra", new UlordURI(MAINNET, BITCOIN_SCHEME + ":"
                + MAINNET_GOOD_ADDRESS + "?label=aardvark=zebra").getLabel());
    }
    
    /**
     * Handles unknown fields (required and not required)
     * 
     * @throws UlordURIParseException
     *             If something goes wrong
     */
    @Test
    public void testUnknown() throws UlordURIParseException {
        // Unknown not required field
        testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?aardvark=true");
        assertEquals("UlordURI['aardvark'='true','address'='1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH']", testObject.toString());

        assertEquals("true", testObject.getParameterByName("aardvark"));

        // Unknown not required field (isolated)
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?aardvark");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("no separator"));
        }

        // Unknown and required field
        try {
            testObject = new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?req-aardvark=true");
            fail("Expecting UlordURIParseException");
        } catch (UlordURIParseException e) {
            assertTrue(e.getMessage().contains("req-aardvark"));
        }
    }

    @Test
    public void brokenURIs() throws UlordURIParseException {
        // Check we can parse the incorrectly formatted URIs produced by blockchain.info and its iPhone app.
        String str = "ulord://1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH?amount=0.01000000";
        UlordURI uri = new UlordURI(str);
        assertEquals("1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH", uri.getAddress().toString());
        assertEquals(CENT, uri.getAmount());
    }

    @Test(expected = UlordURIParseException.class)
    public void testBad_AmountTooPrecise() throws UlordURIParseException {
        new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=0.123456789");
    }

    @Test(expected = UlordURIParseException.class)
    public void testBad_NegativeAmount() throws UlordURIParseException {
        new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=-1");
    }

    @Test(expected = UlordURIParseException.class)
    public void testBad_TooLargeAmount() throws UlordURIParseException {
        new UlordURI(MAINNET, BITCOIN_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=100000000");
    }

    @Test
    public void testPaymentProtocolReq() throws Exception {
        // Non-backwards compatible form ...
        UlordURI uri = new UlordURI(TESTNET, "ulord:?r=https%3A%2F%2Fbitcoincore.org%2F%7Egavin%2Ff.php%3Fh%3Db0f02e7cea67f168e25ec9b9f9d584f9");
        assertEquals("https://bitcoincore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://bitcoincore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9"),
                uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }

    @Test
    public void testMultiplePaymentProtocolReq() throws Exception {
        UlordURI uri = new UlordURI(MAINNET,
                "ulord:?r=https%3A%2F%2Fbitcoincore.org%2F%7Egavin&r1=bt:112233445566");
        assertEquals(ImmutableList.of("bt:112233445566", "https://bitcoincore.org/~gavin"), uri.getPaymentRequestUrls());
        assertEquals("https://bitcoincore.org/~gavin", uri.getPaymentRequestUrl());
    }

    @Test
    public void testNoPaymentProtocolReq() throws Exception {
        UlordURI uri = new UlordURI(MAINNET, "ulord:" + MAINNET_GOOD_ADDRESS);
        assertNull(uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of(), uri.getPaymentRequestUrls());
        assertNotNull(uri.getAddress());
    }

    @Test
    public void testUnescapedPaymentProtocolReq() throws Exception {
        UlordURI uri = new UlordURI(TESTNET,
                "ulord:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe");
        assertEquals("https://merchant.com/pay.php?h=2a8628fc2fbe", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://merchant.com/pay.php?h=2a8628fc2fbe"), uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }
}
