package org.fpwei.game.server.game;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fpwei.game.server.model.Card;
import org.fpwei.game.server.model.Suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Baccarat extends AbstractGameServer {
    /**
     * 剛進遊戲時 回傳資料讓玩家同步用
     */
    //public static int MORA100=100;
    /**
     * 中場休閒 回傳倒數秒數
     */
    public static int FLAG0WAITTIME = 101;
    /**
     * 通知進入下注期
     */
    public static int FLAG0TOFLAG1 = 111;
    /**
     * 下注回傳倒數秒數
     */
    public static int FLAG1WAITTIME = 102;
    /**
     * 下注 回傳大家下注總額
     */
    public static int FLAG1_BACK_ALL_PUT_MONEY = 103;
    /**
     * 下注 告訴自己目前莊閒的壓注情形
     */
    public static int FLAG1_BACK_MY_PUT_MONEY = 104;
    /**
     * 下注 金額不足
     */
    public static int FLAG1_INSUFFICIENT_AMOUNT = 105;
    /**
     * 停止下注
     */
    public static int FLAG2_STOP_PUT = 106;
    /**
     * 閒家第一張牌
     */
    public static int FLAG2_L1 = 107;
    /**
     * 莊家第一張牌
     */
    public static int FLAG2_B1 = 108;
    /**
     * 閒家第二張牌
     */
    public static int FLAG2_L2 = 109;
    /**
     * 莊家第二張牌
     */
    public static int FLAG2_B2 = 110;
    /**
     * 閒家補牌
     */
    public static int FLAG2_L3 = 114;
    /**
     * 莊家補牌
     */
    public static int FLAG2_B3 = 112;
    /**
     * 中場休息 清場
     */
    public static int FLAG0_CLEAN = 113;
    /**
     * 結算 通知誰贏
     */
    public static int FLAG3_WIN = 115;
    /**
     * 結算 有下注的人通知輸贏金額
     */
    public static int FLAG3_PLAY_WIN = 116;
    /**
     * 成功清除下注
     */
    public static int CLEAN_PUT_OK = 117;
    /**
     * 結算等待
     */
    public static int FLAG12_WAIT_RESULT = 118;
    /**
     * 我要下注
     */
    public final static int BACK_PUT_MONEY = 200;
    /**
     * 清除下注
     */
    public final static int BACK_CLEAN_PUT = 201;
    /**
     * 中場休閒 的秒數
     */
    private final static int FLAGWAITTIME0 = 1;
    /**
     * 下注秒數
     */
    private final static int FLAGWAITTIME1 = 3;
    /**
     * 每一張牌的發牌間隔
     */
    private final static int FLAGWAITTIME2 = 1;
    /**
     * 結算後的等待秒數
     */
    private final static int FLAGWAITTIME12 = 1;
    /**
     * 目前第幾局
     */
    private int GameNum;
    /**
     * 撲克牌
     */
    private int[] poker;
    /**
     * 目前發到第幾個位置
     */
    private int nowPoketPos;
    /**
     * 目前桌上的牌
     */
    private int[] stagePoket;
    /**
     * 遊戲進度
     */
    private int GameFlag;

    /**
     * 桌面上的錢0:莊 1:閒
     */
    private int[] stageMoney;
    /**
     * 有下注的人要記起來 別讓他跑了
     */
    private ArrayList<mora_back_data> putMan;
    private int StartTime;
    private HashMap<String, WebClient> UserMap;
    private ArrayList<String> al;
    private static Happy100 main;


    private static final int BETTING_TIME = 10;
    private static final int DEAL_DELAY_SECONDS = 2;

    private long startTime;

    private Card[] bankCards = new Card[3];
    private Card[] playerCards = new Card[3];

    private Card[] shoe = IntStream.rangeClosed(1, 8 * 52)
            .mapToObj(x -> new Card(Suit.getSuit(x / 4), x / 52, x / 10))
            .toArray(Card[]::new);

    @Override
    public void start() {

        new Thread(() -> {
            while (true) {
                if (isPlayerExist()) {
                    startTime = System.currentTimeMillis() + BETTING_TIME * 1000;
                    try {
                        waitForBet();
                        dealFirstTwoCards();
                    } catch (InterruptedException e) {
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }).start();
    }

    /**
     * 等待下注
     */
    private void waitForBet() throws InterruptedException {
        log.debug("等待下注...");
        long remainTime;

        while ((remainTime = startTime - System.currentTimeMillis()) > 0) {
            log.debug("倒數 {} 秒 ...", remainTime);
            broadcast(FLAG1WAITTIME + "!@#" + remainTime / 1000);
            Thread.sleep(500);
        }
    }

    /**
     * 閒家莊家各發兩張牌
     *
     * @throws InterruptedException
     */
    private void dealFirstTwoCards() throws InterruptedException {
        log.debug("開始發牌...");

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        playerCards[0] = shoe[0];
        log.debug("閒家第一張牌 {} ", playerCards[0]);
        broadcast(FLAG2_L1 + "!@#" + playerCards[0]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        bankCards[0] = shoe[1];
        log.debug("莊家第一張牌 {} ", bankCards[0]);
        broadcast(FLAG2_B1 + "!@#" + bankCards[0]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        playerCards[1] = shoe[2];
        log.debug("閒家第二張牌 {} ", playerCards[1]);
        broadcast(FLAG2_L2 + "!@#" + playerCards[1]);

        Thread.sleep(DEAL_DELAY_SECONDS * 1000);
        bankCards[1] = shoe[3];
        log.debug("莊家第二張牌 {} ", bankCards[1]);
        broadcast(FLAG2_B2 + "!@#" + bankCards[1]);
    }

    private boolean checkPair() {
        log.debug("判斷對子");

        boolean isPlayerPair = playerCards[0].getSymbol().equals(playerCards[1].getSymbol());
        boolean isBankPair = bankCards[0].getSymbol().equals(bankCards[1].getSymbol());

        if (isPlayerPair && isBankPair) {
            log.debug("莊家贏 (莊家: {} , 閒家: {})", bankTotal, playerTotal);
            broadcast();
        } else if () {

        }else {

        }
    }

    private boolean checkNatural() {
        log.debug("判斷天牌");
        int playerTotal = (playerCards[0].getValue() + playerCards[1].getValue()) / 10;
        int bankTotal = (bankCards[0].getValue() + bankCards[1].getValue()) / 10;

        if (bankTotal >= 8 || playerTotal >= 8) {
            if (bankTotal > playerTotal) {
                log.debug("莊家贏 (莊家: {} , 閒家: {})", bankTotal, playerTotal);
                broadcast();
            } else if (bankTotal < playerTotal) {
                log.debug("閒家贏 (莊家: {} , 閒家: {})", bankTotal, playerTotal);
                broadcast();
            } else {
                log.debug("和局 (莊家: {} , 閒家: {})", bankTotal, playerTotal);
                broadcast();
            }
            return true;

        } else {
            return false;
        }
    }


    public static Happy100 getMain() {
        if (main == null) {
            main = new Happy100();
        }
        return main;
    }

    public void join(WebClient wc) {
        mora_back_data mbd = new mora_back_data(wc.getPosKey(), wc.playID);
        wc.data = mbd;
        this.putMan.add(mbd);
        this.UserMap.put(wc.getPosKey(), wc);
    }

    /**
     * 放置所有玩家的遊戲資訊
     */
    public Baccarat() {
        this.UserMap = new HashMap();
        this.al = new ArrayList();
        this.putMan = new ArrayList();
        this.poker = new int[416];
        for (int a = 0; a < 8; a++) {
            int c = a * 52;
            for (int b = 0; b < 52; b++) {
                this.poker[c + b] = b;
            }
        }
        this.stagePoket = new int[6];
        this.stageMoney = new int[7];
        this.GameFlag = -1;
        this.GameNum = 0;
        this.GameFlag0();
        new Thread(Judge_Work).start();
        // TODO Auto-generated constructor stub
    }

    private Runnable Judge_Work = new Runnable() {
        public void run() {
            // TODO Auto-generated method stub
            try {
                while (true) {

                    if (al.size() > 0) {
                        process(al.remove(0));
                    }
                    runProcess();
                    Thread.sleep(100);

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    /**
     * 洗牌
     */
    private void Shuffle() {
        for (int a = 0; a < 10; a++) {
            for (int b = 0; b < 416; b++) {
                int c = (int) (Math.random() * 416);
                int d = this.poker[b];
                this.poker[b] = this.poker[c];
                this.poker[c] = d;
            }
        }
        this.nowPoketPos = 0;
    }

    /**
     * 清掉桌上的牌和錢
     */
    private void cleanStagePokerAndMoney() {
        for (int a = 0; a < 6; a++) {
            this.stagePoket[a] = -1;
        }
        this.stageMoney[0] = 0;
        this.stageMoney[1] = 0;
        this.stageMoney[2] = 0;
        Iterator<WebClient> it = this.UserMap.values().iterator();
        while (it.hasNext()) {
            mora_back_data bd = (mora_back_data) (it.next().data);
            bd.cleanPutMoney();
        }
    }

    public void setProcess(String posKey, String str) {
        al.add(posKey + "#@!" + str);
    }

    protected void process(String str) {
        String[] keyArr = str.split("#@!");
        String[] strArr = keyArr[1].split("!@#");
        // TODO Auto-generated method stub
        switch (Integer.parseInt(strArr[0])) {
            case BACK_PUT_MONEY:
                back_put_money(keyArr[0], strArr[1].split(","));
                break;
            case BACK_CLEAN_PUT:
                back_clean_put(keyArr[0]);
        }
    }

    /**
     * 有人下注
     *
     * @param poskey 誰
     * @param str    (莊閒),多少錢
     */
    private void back_put_money(String poskey, String[] str) {
        if (this.GameFlag != 1) {
            return;
        }
        int who = Integer.valueOf(str[0]);
        int money = Integer.valueOf(str[1]);
        double myMoney = 0;
        if (this.UserMap.containsKey(poskey)) {
            myMoney = this.UserMap.get(poskey).money;
        } else {
            return;
        }
        mora_back_data bd = (mora_back_data) this.UserMap.get(poskey).data;

        if (myMoney < bd.getTotalPutMoney() + money) {
            this.UserMap.get(poskey).send(FLAG1_INSUFFICIENT_AMOUNT + "!@#" + bd.getPutMoney());
            return;
        }
        stageMoney[who] += money;
        bd.putMoney(who, money);
        this.UserMap.get(poskey).send(FLAG1_BACK_MY_PUT_MONEY + "!@#" + bd.getPutMoney());
        sayAllTotalPutMoney();
        System.out.println("我下注了");
    }

    private void back_clean_put(String poskey) {
        if (this.GameFlag != 1) {
            return;
        }
        if (!this.UserMap.containsKey(poskey)) {
            return;
        }
        WebClient gd = this.UserMap.get(poskey);
        mora_back_data bd = (mora_back_data) gd.data;
        this.stageMoney[0] -= bd.putMoney[0];
        this.stageMoney[1] -= bd.putMoney[1];
        this.stageMoney[2] -= bd.putMoney[2];
        bd.cleanPutMoney();
        this.putMan.remove(bd);
        this.UserMap.get(poskey).send(CLEAN_PUT_OK + "!@#");
        sayAllTotalPutMoney();
        System.out.println("清除下注");

    }

    /**
     * 告訴大家目前所有的下注金
     */
    private void sayAllTotalPutMoney() {
        sendAll(FLAG1_BACK_ALL_PUT_MONEY + "!@#" + this.stageMoney[0] + "," + this.stageMoney[1]);

    }


    protected void runProcess() {
        // TODO Auto-generated method stub
        switch (this.GameFlag) {
            case 0://休息
                GameFlag0();
                break;
            case 1://下注
                GameFlag1();
                break;
            case 2://開始發牌
                GameFlag2();
                break;
            case 3://閒家第一張牌
                GameFlag3();
                break;
            case 4://莊家第一張牌
                GameFlag4();
                break;
            case 5://閒家第二張牌
                GameFlag5();
                break;
            case 6://莊家第一張牌
                GameFlag6();
                break;
            case 7://判斷閒家是否要補牌 不用會在跳去9
                checkAddPoker();
                break;
            case 9://莊家第一張牌
                checkAddPoker();
                break;
            case 11://結算
                GameFlag11();
                break;
            case 12://結算後 等待
                GameFlag12();
                break;
        }
    }

    /**
     * 進入中場休息
     */
    private void GameFlag0() {
        if (this.GameFlag == 0) {
            int nowTime = (int) (new java.util.Date().getTime() / 1000);
            int waitTime = (this.StartTime - nowTime);
            if (waitTime > 0) {//時間還沒到
                if (this.UserMap.size() > 0) {
                    sendAll(FLAG0WAITTIME + "!@#" + String.valueOf(waitTime));

                } else {
                    this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME0;
                }
            } else {
                if (this.UserMap.size() > 0) {
                    GameFlag1();
                } else {
                    this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME0;
                }
            }
        } else {
            this.GameFlag = 0;
            GameNum++;
            Shuffle();
            cleanStagePokerAndMoney();
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME0;
            sendAll(FLAG0_CLEAN + "!@#");
            System.out.println("進入中場休息");
        }
    }

    public void sendAll(String str) {
        Iterator<WebClient> it = this.UserMap.values().iterator();
        while (it.hasNext()) {
            it.next().send(str);
        }
    }

    /**
     * 進入壓注等待
     */
    private void GameFlag1() {
        if (this.GameFlag == 1) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime >= 0) {//時間還沒到
                sendAll(FLAG1WAITTIME + "!@#" + String.valueOf(waitTime));

            } else {
                GameFlag2();
            }
        } else {
            this.GameFlag = 1;
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME1;
            sendAll(FLAG0TOFLAG1 + "!@#" + String.valueOf(FLAGWAITTIME1));
            System.out.println("進入壓注等待");
        }
    }

    /**
     * 開始發牌
     */
    private void GameFlag2() {
        if (this.GameFlag == 2) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                GameFlag3();
            }
        } else {
            this.GameFlag = 2;
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
            sendAll(FLAG2_STOP_PUT + "!@#");

            System.out.println("開始發牌");
        }
    }

    /**
     * 閒家第一張牌
     */
    private void GameFlag3() {
        if (this.GameFlag == 3) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                GameFlag4();
            }
        } else {
            this.GameFlag = 3;
            this.stagePoket[3] = this.poker[this.nowPoketPos];
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
            sendAll(FLAG2_L1 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));

            this.nowPoketPos++;
            System.out.println("閒家第一張牌");
        }
    }

    /**
     * 莊家第一張牌
     */
    private void GameFlag4() {
        if (this.GameFlag == 4) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                GameFlag5();
            }
        } else {
            this.GameFlag = 4;
            this.stagePoket[0] = this.poker[this.nowPoketPos];
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
            sendAll(FLAG2_B1 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));
            this.nowPoketPos++;
            System.out.println("莊家第一張牌");
        }
    }

    /**
     * 閒家第二張牌
     */
    private void GameFlag5() {
        if (this.GameFlag == 5) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                GameFlag6();
            }
        } else {
            this.GameFlag = 5;
            this.stagePoket[4] = this.poker[this.nowPoketPos];
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
            sendAll(FLAG2_L2 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));
            this.nowPoketPos++;
            System.out.println("閒家第二張牌");
        }
    }

    /**
     * 莊家第二張牌
     */
    private void GameFlag6() {
        if (this.GameFlag == 6) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
                this.GameFlag = 7;
            }
        } else {
            this.GameFlag = 6;
            this.stagePoket[1] = this.poker[this.nowPoketPos];
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
            sendAll(FLAG2_B2 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));
            this.nowPoketPos++;
            System.out.println("莊家第二張牌");
        }
    }

    /**
     * 閒家補牌
     */
    private void GameFlag8() {
        this.stagePoket[5] = this.poker[this.nowPoketPos];
        sendAll(FLAG2_L3 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));
        this.nowPoketPos++;
        this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
        this.GameFlag = 9;
        System.out.println("閒家補牌");
    }

    /**
     * 莊家補牌
     */
    private void GameFlag10() {
        this.stagePoket[2] = this.poker[this.nowPoketPos];
        sendAll(FLAG2_B3 + "!@#" + String.valueOf(this.poker[this.nowPoketPos]));
        this.nowPoketPos++;
        this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME2;
        System.out.println("莊家補牌");
        GameFlag11();
    }

    /**
     * 結算
     */
    private void GameFlag11() {
        if (this.GameFlag == 11) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime < 0) {//時間還沒到
                int L = (PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4]) + PoketToPoint(this.stagePoket[5])) % 10;
                int B = (PoketToPoint(this.stagePoket[0]) + PoketToPoint(this.stagePoket[1]) + PoketToPoint(this.stagePoket[2])) % 10;
                int win = 0;
                if (L > B) {
                    win = 1;
                } else if (B > L) {
                    win = 2;
                } else {
                    win = 0;
                }
                int BDouble = 0;
                int LDouble = 0;
                int BigOrSmall = 0;
                if (this.stagePoket[0] % 13 == this.stagePoket[1] % 13) {
                    BDouble = 1;
                }
                if (this.stagePoket[3] % 13 == this.stagePoket[4] % 13) {
                    LDouble = 1;
                }
                if (this.stagePoket[2] != -1 || this.stagePoket[5] != -1) {
                    BigOrSmall = 1;
                }
                this.sendAll(FLAG3_WIN + "!@#" + String.valueOf(win) + "," + BDouble + "," + LDouble + "," + BigOrSmall);

                for (int a = 0; a < this.putMan.size(); a++) {
                    mora_back_data mbd = this.putMan.get(a);
                    int time = (int) (new java.util.Date().getTime() / 1000);
                    if (mbd.getTotalPutMoney() != 0) {
                        WebClient gud = this.UserMap.get(mbd.poskey);
                        double winMoney = mbd.getWinMoney(win, BigOrSmall, LDouble, BDouble);
                        gud.money += winMoney;
                        //gud.send(FLAG3_PLAY_WIN+"!@#"+winMoney+","+gud.getMoney());
                        MysqlDB.getEntity().SQLUpdate("INSERT INTO user_log(user_id,game_type,win_lose,create_time,money,game_num,room_key)VALUES('" + gud.playID + "','1','" + winMoney + "','" + time + "','" + gud.money + "','" + this.GameNum + "','0')");
                        MysqlDB.getEntity().SQLUpdate("UPDATE user_game_money SET money=" + gud.money + " WHERE user_id ='" + gud.playID + "'");
                        gud.send(FLAG3_PLAY_WIN + "!@#" + winMoney + "," + gud.money);

                    }


                }
                GameFlag12();
            }
        } else {
            this.GameFlag = 11;
            System.out.println("進入結算");
        }
    }

    private void GameFlag12() {
        if (this.GameFlag == 12) {
            int waitTime = this.StartTime - (int) (new java.util.Date().getTime() / 1000);
            if (waitTime >= 0) {//時間還沒到
                sendAll(FLAG12_WAIT_RESULT + "!@#" + String.valueOf(waitTime));

            } else {
                GameFlag0();
            }
        } else {
            this.StartTime = (int) (new java.util.Date().getTime() / 1000) + FLAGWAITTIME12;
            this.GameFlag = 12;
        }
    }

    /**
     * 判斷補牌規則
     */
    private void checkAddPoker() {
        if (this.StartTime - (int) (new java.util.Date().getTime() / 1000) > 0) {
            return;
        }
        if (this.GameFlag == 7) {
            int L = (PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4])) % 10;

            if (L < 6) {
                GameFlag8();//去補牌吧
            } else {
                this.GameFlag = 9;//判斷莊家要不要補牌
            }
        } else if (this.GameFlag == 9) {
            int B = (PoketToPoint(this.stagePoket[0]) + PoketToPoint(this.stagePoket[1])) % 10;
            if (B < 3) {
                GameFlag10();//去補牌吧
            } else if (B == 3) {//如果閒家補得第三張牌（非三張牌點數相加，下同）是8點，不須補牌，其他則需補牌
                if (this.stagePoket[5] != -1) {
                    int L = PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4]) + PoketToPoint(this.stagePoket[5]);
                    if (L == 8) {
                        GameFlag11();
                    } else {
                        GameFlag10();//去補牌吧
                    }
                } else {
                    GameFlag10();//去補牌吧
                }
            } else if (B == 4) {//如果閒家補得第三張牌是0,1,8,9點，不須補牌，其他則需補牌
                if (this.stagePoket[5] != -1) {
                    int L = (PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4]) + PoketToPoint(this.stagePoket[5])) % 10;
                    if (L == 0 || L == 1 || L == 2 || L == 8 || L == 9) {
                        GameFlag11();
                    } else {
                        GameFlag10();//去補牌吧
                    }
                } else {
                    GameFlag10();//去補牌吧
                }
            } else if (B == 5) {//如果閒家補得第三張牌是0,1,2,3,8,9點，不須補牌，其他則需補牌
                if (this.stagePoket[5] != -1) {
                    int L = (PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4]) + PoketToPoint(this.stagePoket[5])) % 10;
                    if (L == 0 || L == 1 || L == 2 || L == 3 || L == 8 || L == 9) {
                        GameFlag11();
                    } else {
                        GameFlag10();//去補牌吧
                    }
                } else {
                    GameFlag10();//去補牌吧
                }
            } else if (B == 6) {//	如果閒家需補牌（即前提是閒家為1至5點）而補得第三張牌是6或7點，補一張牌，其他則不需補牌
                if (this.stagePoket[5] != -1) {
                    int L = (PoketToPoint(this.stagePoket[3]) + PoketToPoint(this.stagePoket[4]) + PoketToPoint(this.stagePoket[5])) % 10;
                    if (L == 6 || L == 7) {
                        GameFlag10();//去補牌吧
                    } else {
                        GameFlag11();
                    }
                } else {
                    GameFlag11();
                }
            } else {

                GameFlag11();
            }
        }
    }

    /**
     * 撲克編號轉點數
     */
    private int PoketToPoint(int num) {
        num = (num % 13) + 1;
        if (num >= 10) {
            num = 0;
        }
        return num;
    }


    /**
     * 在這遊戲裡給客戶存遊戲資訊用
     *
     * @author 滷大師
     */
    class mora_back_data {
        public String poskey;
        public int id;
        public double[] putMoney;

        public mora_back_data(String poskey, int id) {
            this.poskey = poskey;
            this.id = id;
            putMoney = new double[]{0, 0, 0, 0, 0, 0, 0};//0 平手 1閒家WIN 2莊家WIN 3小WIN 4閒對 5 莊對 6大WIN
        }

        public void putMoney(int who, int money) {
            this.putMoney[who] += money;
        }

        public double getTotalPutMoney() {
            return this.putMoney[0] + this.putMoney[1] + this.putMoney[2];
        }

        public String getPutMoney() {
            return this.putMoney[0] + "," + this.putMoney[1] + "," + this.putMoney[2];
        }

        public void cleanPutMoney() {
            this.putMoney[0] = 0;
            this.putMoney[1] = 0;
        }

        public double getWinMoney(int win, int smallorbig, int Ppair, int Bpair) {
            double money = putMoney[0] + putMoney[1] + putMoney[2] + putMoney[3] + putMoney[4] + putMoney[5] + putMoney[6];
            if (money > 0) {//如果有下注
                double winmoney = 0;
                switch (win) {
                    case 0:
                        winmoney = putMoney[2] * 9;
                        break;
                    case 2:
                        winmoney = putMoney[0] * 1.95;
                        break;
                    case 1:
                        winmoney = putMoney[1] * 2;
                        break;
                }
                if (smallorbig == 1) {
                    winmoney += putMoney[6] * 1.05;
                } else {
                    winmoney += putMoney[3] * 2.5;
                }
                if (Ppair == 1) {
                    winmoney += putMoney[4] * 12;
                }
                if (Bpair == 1) {
                    winmoney += putMoney[5] * 12;
                }
                return winmoney - money;
            }
            return 0;
        }
    }

