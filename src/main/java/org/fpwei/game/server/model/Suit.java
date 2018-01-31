package org.fpwei.game.server.model;

public enum Suit {
    CLUBS(0), DIAMONDS(1), HEARTS(2), SPADES(3);


    private int value;

    Suit(int value) {
        this.value = value;
    }

    public static Suit getSuit(int value) {
        switch (value) {
            case 0:
                return CLUBS;
            case 1:
                return DIAMONDS;
            case 2:
                return HEARTS;
            case 3:
                return SPADES;
            default:
                throw new IllegalArgumentException();
        }
    }

    public int getValue() {
        return value;
    }
}
