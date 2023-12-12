package com.monitor.constants;

import org.bouncycastle.pqc.crypto.newhope.NHSecretKeyProcessor;

public class ThirdPartyService {
    public static final String RIPCORD_URL = "https://widget.real-time-reserves.ledgerlens.io/api/v1/truecurrencies/snapshot";

    public static final String RESERVE_URL = "https://api.real-time-reserves.ledgerlens.io/v1/chainlink/proof-of-reserves/TrueUSD";

    public static final String REPORT_URL = "https://widget.real-time-reserves.ledgerlens.io/api/v1/tusd/report?tokens=[TrueUSD]";

    public static final String COIN_MARKET_TRADING_VOLUME = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest";

    public static final String COIN_MARKET_CURRENCY = "https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest";
}
