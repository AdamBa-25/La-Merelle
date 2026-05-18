package model;

import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;
import boardifier.view.ConsoleColor;

/**
 * Représente un pion du jeu de la Mérelle.
 *
 * Couleurs disponibles (constantes PAWN_*) :
 *   PAWN_BLACK  (0) → fond noir,   lettre 'N' blanche
 *   PAWN_RED    (1) → fond rouge,  lettre 'R' noire
 *   PAWN_BLUE   (2) → fond bleu,   lettre 'B' blanche
 *   PAWN_GREEN  (3) → fond vert,   lettre 'V' noire
 *   PAWN_YELLOW (4) → fond jaune,  lettre 'J' noire
 *   PAWN_PURPLE (5) → fond violet, lettre 'M' blanche
 *   PAWN_CYAN   (6) → fond cyan,   lettre 'C' noire
 *
 */
public class MerellePawn extends GameElement {

    // ===== Constantes de couleur =====
    public static final int PAWN_BLACK = 0;
    public static final int PAWN_RED = 1;
    public static final int PAWN_BLUE = 2;
    public static final int PAWN_GREEN = 3;
    public static final int PAWN_YELLOW = 4;
    public static final int PAWN_PURPLE = 5;
    public static final int PAWN_CYAN = 6;

    /** Nombre total de couleurs disponibles — utile pour valider un choix. */
    public static final int NB_COLORS = 7;

    private int color;

    /**
     * @param color L'une des constantes PAWN_* définies ci-dessus.
     * @param gameStageModel Le stage propriétaire de cet élément.
     */
    public MerellePawn(int color, GameStageModel gameStageModel)
    {
        super(gameStageModel);
        ElementTypes.register("pawn", 50);
        type = ElementTypes.getType("pawn");
        this.color = color;
    }

    public int getColor() { return color; }

    /**
     * Lettre représentant ce pion en console (sans mise en forme couleur)
     * Utilisé par exemple pour les logs ou les messages de saisie
     */
    public char getSymbol()
    {
        switch (color)
        {
            case PAWN_BLACK: return 'N';
            case PAWN_RED: return 'R';
            case PAWN_BLUE: return 'B';
            case PAWN_GREEN: return 'V';
            case PAWN_YELLOW: return 'J';
            case PAWN_PURPLE: return 'M';
            case PAWN_CYAN: return 'C';
            default: return '?';
        }
    }

    /**
     * Retourne le nom lisible d'une couleur à partir de son identifiant
     * Statique pour pouvoir être appelée sans instance : MerellePawn.getColorName(colorJ2)
     *
     * @param color l'identifiant de couleur (constante PAWN_*)
     */
    public static String getColorName(int color)
    {
        switch (color)
        {
            case PAWN_BLACK: return "Noir";
            case PAWN_RED: return "Rouge";
            case PAWN_BLUE: return "Bleu";
            case PAWN_GREEN: return "Vert";
            case PAWN_YELLOW: return "Jaune";
            case PAWN_PURPLE: return "Violet";
            case PAWN_CYAN: return "Cyan";
            default: return "Inconnu";
        }
    }

    /**
     * Retourne la chaîne de fond ANSI pour ce pion,
     */
    public String getBackgroundColor()
    {
        switch (color)
        {
            case PAWN_BLACK: return ConsoleColor.BLACK_BACKGROUND;
            case PAWN_RED: return ConsoleColor.RED_BACKGROUND;
            case PAWN_BLUE: return ConsoleColor.BLUE_BACKGROUND;
            case PAWN_GREEN: return ConsoleColor.GREEN_BACKGROUND;
            case PAWN_YELLOW: return ConsoleColor.YELLOW_BACKGROUND;
            case PAWN_PURPLE: return ConsoleColor.PURPLE_BACKGROUND;
            case PAWN_CYAN: return ConsoleColor.CYAN_BACKGROUND;
            default: return ConsoleColor.WHITE_BACKGROUND;
        }
    }

    /**
     * Retourne la couleur du texte ANSI pour ce pion,
     * Les fonds sombres (noir, bleu, violet) reçoivent du texte blanc.
     * Les fonds clairs (rouge, vert, jaune, cyan) reçoivent du texte noir.
     */
    public String getTextColor()
    {
        switch (color)
        {
            case PAWN_BLACK:
            case PAWN_BLUE:
            case PAWN_PURPLE:
                return ConsoleColor.WHITE;
            case PAWN_RED:
            case PAWN_GREEN:
            case PAWN_YELLOW:
            case PAWN_CYAN:
            default:
                return ConsoleColor.BLACK;
        }
    }

    /**
     * Vérifie si un entier correspond à une couleur valide.
     *
     * @param colorId l'entier saisi par l'utilisateur
     * @return true si colorId est entre 0 et NB_COLORS - 1
     */
    public static boolean isValidColor(int colorId)
    {
        return colorId >= 0 && colorId < NB_COLORS;
    }

    /**
     * Affiche dans la console la liste des couleurs disponibles avec leur indice.
     * À appeler dans Merelle.java au moment du choix des couleurs.
     */
    public static void printColorMenu()
    {
        System.out.println("Couleurs disponibles :");
        for (int i = 0; i < NB_COLORS; i++) {
            System.out.println("  " + i + " → " + getColorName(i));
        }
    }
}