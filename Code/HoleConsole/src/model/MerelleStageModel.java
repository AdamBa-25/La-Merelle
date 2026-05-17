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
 * - les 9 pions noirs (joueur 0) et 9 pions rouges (joueur 1)
 * - la phase courante (PLACEMENT ou DEPLACEMENT)
 * - le nombre de pions encore en main (non posés en phase 1)
 * - le flag millJustFormed : une capture est en attente
 * - l'historique des 3 derniers coups pour la règle d'égalité
 * - un TextElement affichant le nom du joueur courant
 *
 * Calqué sur HoleStageModel.java du tutoriel HoleConsole.
 */
public class MerelleStageModel extends GameStageModel {

    /** Phase 1 : placement des pions */
    public static final int PHASE_PLACEMENT   = 0;
    /** Phase 2 : déplacement des pions */
    public static final int PHASE_DEPLACEMENT = 1;

    // --- Éléments du jeu ---
    private MerelleBoard board;
    private MerellePawn[] blackPawns; // 9 pions noirs (joueur 0)
    private MerellePawn[] redPawns;   // 9 pions rouges (joueur 1)
    private TextElement playerName;   // texte affiché au-dessus du plateau

    // --- État de la partie ---
    private int currentPhase;
    private int pawnsInHandBlack; // pions noir pas encore posés
    private int pawnsInHandRed;   // pions rouge pas encore posés
    private boolean millJustFormed; // true si capture en attente
    private String[] lastMoves;     // 3 derniers coups (règle d'égalité)

    /**
     * Constructeur appelé par StageFactory via réflexion.
     *
     * @param name  nom du stage (doit correspondre au nom enregistré dans StageFactory)
     * @param model modèle global
     */
    public MerelleStageModel(String name, Model model) {
        super(name, model);
        currentPhase     = PHASE_PLACEMENT;
        pawnsInHandBlack = 9;
        pawnsInHandRed   = 9;
        millJustFormed   = false;
        lastMoves        = new String[]{null, null, null};
        setupCallbacks();
    }

    /**
     * Configure les callbacks du framework boardifier.
     * Ici on détecte la fin de la phase de placement : dès que tous les pions
     * sont posés, on passe automatiquement en phase de déplacement.
     */
    private void setupCallbacks() {
        onPutInContainer((element, containerDest, rowDest, colDest) -> {
            // On ne s'intéresse qu'aux pions posés sur le plateau
            if (containerDest != board) return;
            // La transition de phase est gérée dans le contrôleur
        });
    }

    // ===== Getters / Setters =====

    public MerelleBoard getBoard() { return board; }
    public void setBoard(MerelleBoard board) {
        this.board = board;
        addContainer(board); // enregistre le plateau comme conteneur du stage
    }

    public MerellePawn[] getBlackPawns() { return blackPawns; }
    public void setBlackPawns(MerellePawn[] pawns) {
        this.blackPawns = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public MerellePawn[] getRedPawns() { return redPawns; }
    public void setRedPawns(MerellePawn[] pawns) {
        this.redPawns = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public TextElement getPlayerName() { return playerName; }
    public void setPlayerName(TextElement playerName) {
        this.playerName = playerName;
        addElement(playerName);
    }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int phase) { this.currentPhase = phase; }

    /** Nombre de pions encore en main pour le joueur (0=noir, 1=rouge). */
    public int getPawnsInHand(int playerId) {
        return (playerId == 0) ? pawnsInHandBlack : pawnsInHandRed;
    }

    /** Décrémente le compteur de pions en main après un placement. */
    public void decreasePawnsInHand(int playerId) {
        if (playerId == 0 && pawnsInHandBlack > 0) pawnsInHandBlack--;
        else if (playerId == 1 && pawnsInHandRed > 0) pawnsInHandRed--;
    }

    public boolean isMillJustFormed() { return millJustFormed; }
    public void setMillJustFormed(boolean formed) { this.millJustFormed = formed; }

    /**
     * Vérifie si tous les pions sont posés et passe en phase de déplacement si c'est le cas.
     * @return true si la transition vient de se produire
     */
    public boolean checkAndTransitionToMovePhase() {
        if (currentPhase == PHASE_PLACEMENT && pawnsInHandBlack == 0 && pawnsInHandRed == 0) {
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
        lastMoves[2] = lastMoves[1];
        lastMoves[1] = lastMoves[0];
        lastMoves[0] = move;
    }

    /**
     * Retourne true si les 3 derniers coups sont identiques (égalité par répétition).
     */
    public boolean isDrawByRepetition() {
        if (lastMoves[0] == null || lastMoves[1] == null || lastMoves[2] == null) return false;
        return lastMoves[0].equals(lastMoves[1]) && lastMoves[1].equals(lastMoves[2]);
    }

    @Override
    public StageElementsFactory getDefaultElementFactory() {
        return new MerelleStageFactory(this);
    }
}
