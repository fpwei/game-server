package org.fpwei.game.server.game;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fpwei.game.server.common.CommonRuntimeException;
import org.fpwei.game.server.message.BetRequest;
import org.fpwei.game.server.message.Request;
import org.fpwei.game.server.model.Card;
import org.fpwei.game.server.model.Player;
import org.fpwei.game.server.model.Suit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.math.BigDecimal.ZERO;

@Slf4j
public class Baccarat extends AbstractGameServer {

    private static final BigDecimal BANKER_WIN_ODDS = new BigDecimal(0.95);
    private static final BigDecimal PLAYER_WIN_ODDS = new BigDecimal(1);
    private static final BigDecimal BANKER_PAIR_ODDS = new BigDecimal(11);
    private static final BigDecimal PLAYER_PAIR_ODDS = new BigDecimal(11);
    private static final BigDecimal TIE_ODDS = new BigDecimal(8);
    private static final BigDecimal BIG_ODDS = new BigDecimal(0.53);
    private static final BigDecimal SMALL_ODDS = new BigDecimal(1.45);


    private static final int BETTING_TIME = 10;
    private static final int DEAL_DELAY_SECONDS = 3;

    private static final int[][] BANKER_RULE = {
/*          Player Third Card               /Banker's
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9     Score
*/
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //0
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //1
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //2
            {1, 1, 1, 1, 1, 1, 1, 1, 0, 1}, //3
            {0, 0, 1, 1, 1, 1, 1, 1, 0, 0}, //4
            {0, 0, 0, 0, 1, 1, 1, 1, 0, 0}, //5
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 0}, //6
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //7
    };

    private final static String BET_TYPE = "betType";
    private final static String BET_CATEGORY = "betCategory";

    private boolean isBetEnabled;
    private long startTime;

    private Card[] bankerCards;
    private Card[] playerCards;

    private Card[] shoe = IntStream.rangeClosed(1, 8 * 52)
            .mapToObj(x -> {
                int rank = x % 13 + 1;
                return new Card(Suit.getSuit(x % 4), rank, (rank) / 10 == 1 ? 0 : rank);
            })
            .toArray(Card[]::new);

    private int position = shoe.length;

    private final List<Bet> betList = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    public Baccarat(@Qualifier("betExecutor") TaskExecutor taskExecutor) {
        super(taskExecutor);
        this.start();
    }


    @Override
    public void start() {

        new Thread(() -> {
            while (true) {
                if (isPlayerExist()) {
                    try {
                        broadcast("init");
                        initialize();
                        isBetEnabled = true;
                        waitForBet();
                        broadcast("stop");
                        isBetEnabled = false;
                        dealFirstTwoCards();
                        if (!isResultConfirmed()) {
                            dealThirdCardToPlayer();
                            dealThirdCardToBanker();
                        }
                        Result result = getResult();
                        broadcast("result:" + result.toString());

                        sendAward(result);
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }).start();
    }

    @Override
    protected boolean process0(Player player, Request request) {
        if (request instanceof BetRequest) {
            return bet(player, (BetRequest) request);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public boolean bet(Player player, BetRequest request) {
        if (!isBetEnabled) {
            return false;
        }

        if (player.getBalance().compareTo(request.getAmount()) < 0) {
            throw new CommonRuntimeException("餘額不足");
        }

        Bet bet = betList.stream().filter(b -> b.player.equals(player)).findAny().orElse(null);

        if (bet == null) {
            bet = new Bet(player);
            betList.add(bet);
        }

        String type = request.getAttribute(BET_TYPE);
        String category = request.getAttribute(BET_CATEGORY);

        BigDecimal betAmount;
        if ("RAISE".equals(type)) {
            betAmount = request.getAmount();
            player.getUser().setAmount(player.getUser().getAmount().subtract(betAmount));
        } else if ("CANCEL".equals(type)) {
            betAmount = request.getAmount().negate();
        } else {
            throw new CommonRuntimeException("Illegal bet type");
        }

        switch (category) {
            case "BW":
                bet.raiseBankerWin(betAmount);
                break;
            case "BP":
                bet.raiseBankerPair(betAmount);
                break;
            case "PW":
                bet.raisePlayerWin(betAmount);
                break;
            case "PP":
                bet.raisePlayerPair(betAmount);
                break;
            case "T":
                bet.raiseTie(betAmount);
                break;
            case "B":
                bet.raiseBig(betAmount);
                break;
            case "S":
                bet.raiseSmall(betAmount);
                break;
            default:
                throw new CommonRuntimeException("Illegal bet category");
        }

        return true;
    }

    private void initialize() {
        bankerCards = new Card[3];
        playerCards = new Card[3];

        if ((position + 6) - 1 > shoe.length) {
            shuffle();
        }

        betList.clear();
    }

    /**
     * 洗牌
     */
    private void shuffle() {
        for (int i = 0; i < shoe.length; i++) {
            int j = (int) (Math.random() * 416);
            Card temp = this.shoe[i];
            this.shoe[i] = this.shoe[j];
            this.shoe[j] = temp;
        }

        position = 0;
    }

    /**
     * 等待下注
     */
    private void waitForBet() throws InterruptedException {
        log.debug("等待下注...");
        long remainTime;

        broadcast("開放下注");
        Thread.sleep(1000);

        startTime = System.currentTimeMillis() + BETTING_TIME * 1000;
        while ((remainTime = startTime - System.currentTimeMillis()) > 0) {
            log.debug("倒數 {} 秒 ...", remainTime / 1000);
            broadcast(String.format("下注倒數%s秒...", remainTime / 1000));
            Thread.sleep(500);
        }

        broadcast("停止下注");
        Thread.sleep(1000);
    }

    /**
     * 閒家莊家各發兩張牌
     *
     * @throws InterruptedException
     */
    private void dealFirstTwoCards() throws InterruptedException {
        log.debug("開始發牌...");

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        playerCards[0] = shoe[position++];
        log.debug("閒家第一張牌 {} ", playerCards[0]);
        broadcast("P#1:" + playerCards[0]);
        broadcast("閒家第一張牌: " + playerCards[0]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        bankerCards[0] = shoe[position++];
        log.debug("莊家第一張牌 {} ", bankerCards[0]);
        broadcast("B#1:" + bankerCards[0]);
        broadcast("莊家第一張牌: " + bankerCards[0]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        playerCards[1] = shoe[position++];
        log.debug("閒家第二張牌 {} ", playerCards[1]);
        broadcast("P#2:" + playerCards[1]);
        broadcast("閒家第二張牌: " + playerCards[1]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        bankerCards[1] = shoe[position++];
        log.debug("莊家第二張牌 {} ", bankerCards[1]);
        broadcast("B#2:" + bankerCards[1]);
        broadcast("莊家第二張牌: " + bankerCards[1]);
    }


    private boolean isResultConfirmed() {
        log.debug("判斷能否決定勝負...");
        int playerTotal = getTotal(playerCards);
        int bankerTotal = getTotal(bankerCards);

        if (bankerTotal >= 8 || playerTotal >= 8) {
            log.debug("有一方取得天牌");
            return true;
        } else if ((bankerTotal == 6 || bankerTotal == 7) && (playerTotal == 6 || playerTotal == 7)) {
            log.debug("雙方皆不需補牌");
            return true;
        } else {
            return false;
        }
    }

    private void dealThirdCardToPlayer() throws InterruptedException {
        if (playerCards[0].getValue() + playerCards[1].getValue() < 7) {
            Thread.sleep(DEAL_DELAY_SECONDS * 1000);
            playerCards[2] = shoe[position++];
            log.debug("閒家第三張牌 {} ", playerCards[2]);
            broadcast("P#3:" + playerCards[2]);
            broadcast("閒家第三張牌: " + playerCards[2]);
        }
    }

    private void dealThirdCardToBanker() throws InterruptedException {

        int playerTotal = getTotal(playerCards);
        int bankerTotal = getTotal(bankerCards);

        if (BANKER_RULE[bankerTotal][playerTotal] == 1) {
            Thread.sleep(DEAL_DELAY_SECONDS * 1000);
            bankerCards[2] = shoe[position++];
            log.debug("莊家第三張牌 {} ", bankerCards[2]);
            broadcast("B#3:" + bankerCards[2]);
            broadcast("莊家第三張牌: " + bankerCards[2]);
        }
    }

    private Result getResult() {
        Result result = new Result();

        result.isSmall = (playerCards[2] == null && bankerCards[2] == null);
        result.isBig = !result.isSmall;
        result.isPlayerPair = (playerCards[0].getSymbol().equals(playerCards[1].getSymbol()));
        result.isBankerPair = (bankerCards[0].getSymbol().equals(bankerCards[1].getSymbol()));

        int playerTotal = getTotal(playerCards);
        int bankerTotal = getTotal(bankerCards);

        result.isPlayerWin = (playerTotal > bankerTotal);
        result.isBankerWin = (playerTotal < bankerTotal);
        result.isTie = (playerTotal == bankerTotal);

        log.debug("結算: {莊贏: {}, 莊對: {}, 閒贏: {}, 閒對: {}, 和局: {}, 開大: {}, 開小 {}}\n(莊家: {{}, {}, {}}, 閒家: {{}, {}, {}})",
                result.isBankerWin, result.isBankerPair, result.isPlayerWin, result.isPlayerPair, result.isTie, result.isBig, result.isSmall,
                bankerCards[0], bankerCards[1], bankerCards[2], playerCards[0], playerCards[1], playerCards[2]
        );

        return result;
    }


    private void sendAward(final Result result) {
        broadcast("派彩...");
        betList.forEach(bet -> {
            BigDecimal winAmount = bet.getWinAmount(result).setScale(2, RoundingMode.DOWN);
            sendMessage(bet.player, "win: " + winAmount.toString());
            bet.player.getUser().setAmount(bet.player.getUser().getAmount().add(winAmount));
            sendUserInfo(bet.player);
            //TODO update wallet

        });
    }

    private int getTotal(Card[] cards) {
        return Arrays.stream(cards)
                .mapToInt(card -> card == null ? 0 : card.getValue())
                .sum() % 10;
    }


    private class Result {
        boolean isPlayerWin;
        boolean isBankerWin;
        boolean isPlayerPair;
        boolean isBankerPair;
        boolean isTie;
        boolean isBig;
        boolean isSmall;

        void initial() {
            isPlayerWin = isPlayerPair = isBankerWin = isBankerPair = isTie = isBig = isSmall = false;
        }

        @Override
        public String toString() {
//            return String.format("結算: {莊贏: %s, 莊對: %s, 閒贏: %s, 閒對: %s, 和局: %s, 開大: %s, 開小 %s}",
//                    isBankerWin, isBankerPair, isPlayerWin, isPlayerPair, isTie, isBig, isSmall);

            return StringUtils.stripEnd(((isPlayerWin ? "PW," : "") + (isBankerWin ? "BW," : "") + (isPlayerPair ? "PP," : "") + (isBankerPair ? "BP," : "") +
                    (isTie ? "T," : "") + (isBig ? "B," : "") + (isSmall ? "S," : "")), ",");
        }
    }

    private class Bet {
        Player player;

        BigDecimal bankerWinBetAmount;
        BigDecimal bankerPairBetAmount;
        BigDecimal playerWinBetAmount;
        BigDecimal playerPairBetAmount;
        BigDecimal tieBetAmount;
        BigDecimal bigWinBetAmount;
        BigDecimal smallWinBetAmount;

        private Bet(Player player) {
            this.player = player;
            this.bankerWinBetAmount = ZERO;
            this.bankerPairBetAmount = ZERO;
            this.playerWinBetAmount = ZERO;
            this.playerPairBetAmount = ZERO;
            this.tieBetAmount = ZERO;
            this.bigWinBetAmount = ZERO;
            this.smallWinBetAmount = ZERO;
        }

        void raiseBankerWin(BigDecimal amount) {
            bankerWinBetAmount = bankerWinBetAmount.add(amount);
        }

        void raiseBankerPair(BigDecimal amount) {
            bankerPairBetAmount = bankerPairBetAmount.add(amount);
        }

        void raisePlayerWin(BigDecimal amount) {
            playerWinBetAmount = playerWinBetAmount.add(amount);
        }

        void raisePlayerPair(BigDecimal amount) {
            playerPairBetAmount = playerPairBetAmount.add(amount);
        }

        void raiseTie(BigDecimal amount) {
            tieBetAmount = tieBetAmount.add(amount);
        }

        void raiseBig(BigDecimal amount) {
            bigWinBetAmount = bigWinBetAmount.add(amount);
        }

        void raiseSmall(BigDecimal amount) {
            smallWinBetAmount = smallWinBetAmount.add(amount);
        }

        BigDecimal getWinAmount(Result result) {
            BigDecimal winAmount = ZERO;
            if (result.isBankerWin && bankerWinBetAmount.compareTo(ZERO) > 0) {
                winAmount = winAmount.add(bankerWinBetAmount.add(bankerWinBetAmount.multiply(BANKER_WIN_ODDS)));
            } else if (result.isPlayerWin && playerWinBetAmount.compareTo(ZERO) > 0) {
                winAmount = winAmount.add(playerWinBetAmount.add(playerWinBetAmount.multiply(PLAYER_WIN_ODDS)));
            } else {
                winAmount = winAmount.add(tieBetAmount.add(tieBetAmount.multiply(TIE_ODDS)));
            }

            if (result.isBankerPair && bankerPairBetAmount.compareTo(ZERO) > 0) {
                winAmount = winAmount.add(bankerPairBetAmount.add(bankerPairBetAmount.multiply(BANKER_PAIR_ODDS)));
            }

            if (result.isPlayerPair && playerPairBetAmount.compareTo(ZERO) > 0) {
                winAmount = winAmount.add(playerPairBetAmount.add(playerPairBetAmount.multiply(PLAYER_PAIR_ODDS)));
            }

            if (result.isBig && bigWinBetAmount.compareTo(ZERO) > 0) {
                winAmount = winAmount.add(bigWinBetAmount.add(bigWinBetAmount.multiply(BIG_ODDS)));
            } else {
                winAmount = winAmount.add(smallWinBetAmount.add(smallWinBetAmount.multiply(SMALL_ODDS)));
            }

            return winAmount;
        }
    }

}
