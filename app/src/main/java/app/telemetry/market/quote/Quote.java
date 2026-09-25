package app.telemetry.market.quote;

public final class Quote {
    public final String symbol;
    public final String name;
    public final double price;
    public final double changePct;
    public final float[] spark;
    public final String session;
    public final double weekHigh;
    public final double weekLow;

    public Quote(String symbol, String name, double price, double changePct, float[] spark, String session) {
        this(symbol, name, price, changePct, spark, session, 0, 0);
    }

    public Quote(
            String symbol, String name, double price, double changePct, float[] spark, String session,
            double weekHigh, double weekLow) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.changePct = changePct;
        this.spark = spark;
        this.session = session;
        this.weekHigh = weekHigh;
        this.weekLow = weekLow;
    }

    public int weekPct() {
        if (weekHigh <= weekLow || price <= 0) return -1;
        return (int) Math.round((price - weekLow) / (weekHigh - weekLow) * 100.0);
    }
}
