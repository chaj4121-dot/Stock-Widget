package app.telemetry.market.quote;

import java.util.Locale;

import app.telemetry.market.launch.Launch;

public final class TickerCopy {
    private TickerCopy() {}

    public static String koreanName(String symbol) {
        if (symbol == null) return "";
        switch (symbol.toUpperCase(Locale.US)) {
            case "META":
                return "메타";
            case "NVDA":
                return "엔비디아";
            case "AAPL":
                return "애플";
            case "MSFT":
                return "마이크로소프트";
            case "GOOGL":
            case "GOOG":
                return "알파벳";
            case "AMZN":
                return "아마존";
            case "TSLA":
                return "테슬라";
            case "SPCX":
            case "SPACE":
                return "스페이스X";
            case "UNH":
                return "유나이티드헬스";
            case "NFLX":
                return "넷플릭스";
            case "PLTR":
                return "팔란티어";
            case "AVGO":
                return "브로드컴";
            case "JPM":
                return "JP모건";
            case "SPY":
                return "미국 500";
            case "QQQ":
                return "나스닥 100";
            case "BRK-B":
            case "BRK-A":
                return "버크셔";
            case "RKLB":
                return "로켓랩";
            default:
                return symbol;
        }
    }

    public static String story(Quote q, Launch launch) {
        if (q == null) return "주말엔 시세 대신 자리를 봐요.";
        String name = koreanName(q.symbol);
        if (Launch.isLaunchTicker(q.symbol) && launch != null) {
            return name + ". 다음 발사까지 " + koreanCountdown(launch.netMs) + ". " + koreanMission(launch) + ".";
        }
        String biz = bizLine(q.symbol);
        String seat = seatLine(q);
        String last = lastLine(q);
        return name + ". " + biz + " " + seat + " " + last;
    }

    public static String koreanCountdown(long netMs) {
        long s = (netMs - System.currentTimeMillis()) / 1000L;
        if (s <= 0) return "거의 직전이에요";
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        if (d >= 2) return d + "일 " + h + "시간 남았어요";
        if (d == 1) return "하루 " + h + "시간 남았어요";
        if (h >= 1) return h + "시간 " + m + "분 남았어요";
        return m + "분 남았어요";
    }

    public static String koreanMission(Launch launch) {
        String m = launch.mission + " " + launch.name;
        String lower = m.toLowerCase(Locale.US);
        if (lower.contains("starlink")) {
            String num = m.replaceAll("(?i).*?(starlink\\s*(group\\s*)?)", "");
            num = num.replaceAll("[^0-9\\-].*", "").trim();
            return num.isEmpty() ? "스타링크 발사" : "스타링크 " + num;
        }
        if (lower.contains("starship") || lower.contains("flight")) return "스타십 시험비행";
        if (lower.contains("crew")) return "유인 발사";
        if (lower.contains("ussf")) return "미 우주군 임무";
        return launch.shortMission();
    }

    private static String bizLine(String symbol) {
        switch (symbol.toUpperCase(Locale.US)) {
            case "META":
                return "광고와 리얼이 이 가격을 받쳐요.";
            case "NVDA":
                return "칩 수요가 자리를 만들었어요.";
            case "AAPL":
                return "아이폰과 서비스가 중심이에요.";
            case "MSFT":
                return "클라우드와 오피스가 버텨요.";
            case "GOOGL":
            case "GOOG":
                return "검색·유튜브·클라우드가 같이 굴러가요.";
            case "AMZN":
                return "쇼핑과 클라우드가 한 몸이에요.";
            case "TSLA":
                return "차·에너지·로봇을 묶어 봐요.";
            case "SPCX":
            case "SPACE":
                return "발사와 스타링크가 이 이름이에요.";
            case "UNH":
                return "미국 의료보험이 본업이에요.";
            case "NFLX":
                return "구독 영상이 실적의 전부예요.";
            case "PLTR":
                return "데이터 소프트웨어가 깔려 있어요.";
            case "BRK-B":
            case "BRK-A":
                return "보험과 현금이 받쳐 주는 투자회사예요.";
            case "SPY":
                return "미국 큰 기업을 한 번에 담아요.";
            case "QQQ":
                return "나스닥 큰 기술주를 묶어 둔 상품이에요.";
            case "JPM":
                return "미국 은행의 중심축이에요.";
            default:
                return "주말엔 시세 대신 자리를 봐요.";
        }
    }

    private static String seatLine(Quote q) {
        int p = q.weekPct();
        if (p < 0) return "";
        if (p >= 85) return "일 년 자리 중 꼭대기 근처예요.";
        if (p >= 65) return "일 년 자리 중 위쪽에 있어요.";
        if (p >= 40) return "일 년 자리 한가운데에서 쉬고 있어요.";
        if (p >= 20) return "일 년 자리 중 아래쪽을 보고 있어요.";
        return "일 년 저점 근처에 붙어 있어요.";
    }

    private static String lastLine(Quote q) {
        if (q.price <= 0) return "";
        if (q.changePct > 2) return "직전 장에서 꽤 올랐어요.";
        if (q.changePct > 0.4) return "직전 장은 소폭 상승.";
        if (q.changePct < -2) return "직전 장에서 많이 밀렸어요.";
        if (q.changePct < -0.4) return "직전 장은 살짝 약했어요.";
        return "직전 종가는 거의 제자리예요.";
    }
}
