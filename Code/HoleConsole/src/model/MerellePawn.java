package model;

import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;

/**
 * Représente un pion du jeu de la Mérelle.
 * Chaque pion a une couleur : PAWN_BLACK (joueur 1) ou PAWN_RED (joueur 2).
 * Calqué sur Pawn.java du tutoriel HoleConsole.
 */
public class MerellePawn extends GameElement {

    public static final int PAWN_BLACK = 0;
    public static final int PAWN_RED   = 1;

    private int color;

    /**
     * @param color PAWN_BLACK ou PAWN_RED
     * @param gameStageModel le stage propriétaire de cet élément
     */
    public MerellePawn(int color, GameStageModel gameStageModel) {
        super(gameStageModel);
        ElementTypes.register("pawn", 50);
        type = ElementTypes.getType("pawn");
        this.color = color;
    }

    public int getColor() { return color; }

    /**
     * Symbole affiché en console : 'N' (Noir) ou 'R' (Rouge).
     */
    public char getSymbol() {
        return (color == PAWN_BLACK) ? 'N' : 'R';
    }
}
