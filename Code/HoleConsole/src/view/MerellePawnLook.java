package view;

import boardifier.model.GameElement;
import boardifier.view.ConsoleColor;
import boardifier.view.ElementLook;
import model.MerellePawn;

/**
 * Look console d'un pion de la Mérelle.
 *
 * Affiche 1 caractère coloré :
 *  - Joueur Noir : fond noir, lettre 'N' blanche
 *  - Joueur Rouge : fond rouge, lettre 'R' noire
 *
 * Calqué sur PawnLook.java du tutoriel HoleConsole.
 */
public class MerellePawnLook extends ElementLook {

    /**
     * @param element le MerellePawn associé à ce look
     */
    public MerellePawnLook(GameElement element) {
        super(element, 1, 1);
    }

    @Override
    protected void render()
    {
        MerellePawn pawn = (MerellePawn) element;
        shape[0][0] = pawn.getTextColor() + pawn.getBackgroundColor() + pawn.getSymbol() + ConsoleColor.RESET;
    }
}
