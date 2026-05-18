package model;

import boardifier.model.GameStageModel;
import boardifier.model.Model;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Modèle du stage de la Mérelle.
 *
 * Contient l'état complet d'une partie :
 * - le plateau (MerelleBoard)
 * - les 9 pions joueur 1 et 9 pions joueur 2
 * - la phase courante (PLACEMENT ou DEPLACEMENT)
 * - le nombre de pions encore en main (non posés en phase 1)
 * - le flag millJustFormed : une capture est en attente
 * - l'historique des derniers coups pour la règle d'égalité
 * - un TextElement affichant le nom du joueur courant
 */
public class MerelleStageModel extends GameStageModel {

    /** Phase 1 : placement des pions */
    public static final int PHASE_PLACEMENT   = 0;
    /** Phase 2 : déplacement des pions */
    public static final int PHASE_DEPLACEMENT = 1;

    /** Nombre de coups mémorisés pour la règle d'égalité par répétition */
    private static final int DRAW_HISTORY_SIZE = 3;

    // --- Éléments du jeu ---
    private MerelleBoard board;
    private MerellePawn[] pawnsJ1; // 9 pions du joueur 0
    private MerellePawn[] pawnsJ2; // 9 pions du joueur 1
    private TextElement playerName; // texte affiché au-dessus du plateau

    // --- État de la partie ---
    private int currentPhase;
    private int pawnsInHandJ1; // pions joueur 0 pas encore posés
    private int pawnsInHandJ2; // pions joueur 1 pas encore posés
    private boolean millJustFormed; // true si une capture est en attente
    private String[] lastMoves; // historique des derniers coups

    /**
     * Constructeur appelé par StageFactory via réflexion.
     *
     * @param name  nom du stage (doit correspondre au nom enregistré dans StageFactory)
     * @param model modèle global
     */
    public MerelleStageModel(String name, Model model) {
        super(name, model);
        currentPhase   = PHASE_PLACEMENT;
        pawnsInHandJ1  = 9;
        pawnsInHandJ2  = 9;
        millJustFormed = false;
        lastMoves      = new String[DRAW_HISTORY_SIZE];
    }

    // ===== Getters / Setters =====

    public MerelleBoard getBoard() { return board; }
    public void setBoard(MerelleBoard board) {
        this.board = board;
        addContainer(board);
    }

    public MerellePawn[] getPawnsJ1() { return pawnsJ1; }
    public void setPawnsJ1(MerellePawn[] pawns) {
        this.pawnsJ1 = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public MerellePawn[] getPawnsJ2() { return pawnsJ2; }
    public void setPawnsJ2(MerellePawn[] pawns) {
        this.pawnsJ2 = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public TextElement getPlayerName() { return playerName; }
    public void setPlayerName(TextElement playerName) {
        this.playerName = playerName;
        addElement(playerName);
    }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int phase) { this.currentPhase = phase; }

    public boolean isMillJustFormed() { return millJustFormed; }
    public void setMillJustFormed(boolean formed) { this.millJustFormed = formed; }

    /** Retourne true si tous les pions des deux joueurs sont posés. */
    public boolean allPawnsPlaced() {
        return pawnsInHandJ1 == 0 && pawnsInHandJ2 == 0;
    }

    /** Nombre de pions encore en main pour le joueur donné. */
    public int getPawnsInHand(int playerId) {
        return (playerId == 0) ? pawnsInHandJ1 : pawnsInHandJ2;
    }

    /** Décrémente le compteur de pions en main après un placement. */
    public void decreasePawnsInHand(int playerId) {
        if (playerId == 0 && pawnsInHandJ1 > 0) pawnsInHandJ1--;
        else if (playerId == 1 && pawnsInHandJ2 > 0) pawnsInHandJ2--;
    }

    /**
     * Vérifie si tous les pions sont posés et passe en phase de déplacement.
     * @return true si la transition vient de se produire
     */
    public boolean checkAndTransitionToMovePhase() {
        if (currentPhase == PHASE_PLACEMENT && allPawnsPlaced()) {
            currentPhase = PHASE_DEPLACEMENT;
            return true;
        }
        return false;
    }

    /**
     * Enregistre le dernier coup joué pour la règle d'égalité par répétition.
     * @param move description du coup (ex. "place:4", "9->10", "capture:2")
     */
    public void recordMove(String move) {
        for (int i = DRAW_HISTORY_SIZE - 1; i > 0; i--) {
            lastMoves[i] = lastMoves[i - 1];
        }
        lastMoves[0] = move;
    }

    /**
     * Retourne true si les derniers coups sont tous identiques (égalité par répétition).
     */
    public boolean isDrawByRepetition() {
        if (lastMoves[0] == null) return false;
        for (int i = 1; i < DRAW_HISTORY_SIZE; i++) {
            if (lastMoves[i] == null || !lastMoves[i].equals(lastMoves[0])) return false;
        }
        return true;
    }

    @Override
    public StageElementsFactory getDefaultElementFactory() {
        return new MerelleStageFactory(this);
    }
}