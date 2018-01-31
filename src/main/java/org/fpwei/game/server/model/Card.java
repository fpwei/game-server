package org.fpwei.game.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Card {
    private Suit suit;
    private String symbol;
    private int value;


    public Card(Suit suit, int rank, int value) {
        this.suit = suit;
        this.value = value;

        switch (rank) {
            case 1:
                this.symbol = "A";
                break;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                this.symbol = String.valueOf(rank);
                break;
            case 11:
                this.symbol = "J";
                break;
            case 12:
                this.symbol = "Q";
                break;
            case 13:
                this.symbol = "K";
                break;
            default:
                throw new IllegalArgumentException();
        }

    }


}
