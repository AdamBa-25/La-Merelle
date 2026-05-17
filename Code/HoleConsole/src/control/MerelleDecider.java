package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.control.ActionFactory;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Décideur IA pour le jeu de la Mérelle.
 *
 * Stratégie en priorités décroissantes :
 * 1. Former un moulin (placement ou déplacement)
 * 2. Bloquer un moulin adverse en formation (2 pions + 1 case libre)
 * 3. Occuper une case stratégique (centres des lignes = mieux connectés)
 * 4. Mouvement aléatoire parmi les coups valides
 *
 * Pour la capture, priorité au pion adverse hors moulin avec le plus de connectivité.
 *
 * Calqué sur HoleDecider.java du tutoriel HoleConsole.
 */
public class MerelleDecider extends Decider {

    private static final Random random = new Random();

    public MerelleDecider(Model model, Controller control) {
        super(model, control);
    }

    /**
     * Retourne une ActionList vide : la décision réelle est obtenue via getDecision()
     * et appliquée directement dans le contrôleur (pattern console sans animation).
     */
    @Override
    public ActionList decide() {
        return new ActionList();
    }

    /**
     * Calcule et retourne la décision de l'IA sous forme de chaîne,
     * dans le même format que la saisie clavier humaine.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   identifiant du joueur IA (0 ou 1)
     * @return la saisie simulée (ex. "A1", "A1 B1", "XA1")
     */
    public String getDecision(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();

        // Si une capture est obligatoire (moulin vient d'être formé)
        if (stageModel.isMillJustFormed()) {
            return decideCapture(board, playerId);
        }
        // Phase 1 : placement
        if (stageModel.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) {
            return decidePlacement(board, playerId);
        }
        // Phase 2 : déplacement
        return decideMove(board, playerId);
    }

    // ================================================================
    // PHASE 1 : PLACEMENT
    // ================================================================

    /**
     * Décide où placer un pion (phase 1).
     * Priorités : compléter un moulin > bloquer > case stratégique > aléatoire.
     */
    private String decidePlacement(MerelleBoard board, int playerId) {
        // 1. Compléter un moulin propre
        int pos = findMillCompletion(board, playerId);
        if (pos >= 0) return posToCoord(pos);

        // 2. Bloquer un moulin adverse
        pos = findMillCompletion(board, 1 - playerId);
        if (pos >= 0 && board.isFreeAt(pos)) return posToCoord(pos);

        // 3. Cases stratégiques (milieux des lignes = plus connectées)
        int[] strategic = {4, 10, 13, 19, 1, 7, 16, 22, 0, 2, 3, 5, 6, 8, 9, 11, 12, 14, 15, 17, 18, 20, 21, 23};
        for (int p : strategic)
            if (board.isFreeAt(p)) return posToCoord(p);

        // 4. Aléatoire
        return posToCoord(randomFree(board));
    }

    // ================================================================
    // PHASE 2 : DÉPLACEMENT
    // ================================================================

    /**
     * Décide quel pion déplacer et vers quelle case (phase 2).
     * Priorités : former un moulin > bloquer > aléatoire.
     */
    private String decideMove(MerelleBoard board, int playerId) {
        List<int[]> moves = allPossibleMoves(board, playerId);
        if (moves.isEmpty()) return null; // bloqué, détecté dans le contrôleur

        // 1. Mouvement formant un moulin
        for (int[] mv : moves) {
            MerellePawn pawn = board.getPawnAt(mv[0]);
            // Simulation : retire et place temporairement
            board.removePawnAt(mv[0]);
            board.placePawnAt(pawn, mv[1]);
            boolean mill = board.checkMillFormed(mv[1], playerId);
            // Annulation
            board.removePawnAt(mv[1]);
            board.placePawnAt(pawn, mv[0]);
            if (mill) return posToCoord(mv[0]) + " " + posToCoord(mv[1]);
        }

        // 2. Mouvement bloquant un moulin adverse (dest adjacente à 2 pions adverses)
        for (int[] mv : moves) {
            if (nearOpponentMill(board, mv[1], 1 - playerId))
                return posToCoord(mv[0]) + " " + posToCoord(mv[1]);
        }

        // 3. Aléatoire
        int[] mv = moves.get(random.nextInt(moves.size()));
        return posToCoord(mv[0]) + " " + posToCoord(mv[1]);
    }

    // ================================================================
    // CAPTURE
    // ================================================================

    /**
     * Décide quel pion adverse capturer.
     * Priorité au pion hors moulin le plus connecté (le plus dangereux).
     * Si tous les adversaires sont en moulin, capture quand même (règle officielle).
     */
    private String decideCapture(MerelleBoard board, int playerId) {
        int opp = 1 - playerId;
        boolean allInMills = board.allPawnsInMills(opp);

        List<Integer> targets = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == opp) {
                if (allInMills || !board.isInMill(pos, opp))
                    targets.add(pos);
            }
        }

        // Choisit le pion avec le plus de voisins libres (menace future maximale)
        int best = -1, bestConn = -1;
        for (int pos : targets) {
            int conn = 0;
            for (int adj : MerelleBoard.ADJACENCY[pos])
                if (board.isFreeAt(adj)) conn++;
            if (conn > bestConn) { bestConn = conn; best = pos; }
        }

        return "X" + posToCoord(best >= 0 ? best : targets.get(0));
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================

    /**
     * Cherche une case libre qui complèterait un moulin pour playerId
     * (moulin avec 2 pions du joueur + 1 case libre).
     *
     * @return la position libre, ou -1 si aucun moulin presque complet
     */
    private int findMillCompletion(MerelleBoard board, int playerId) {
        for (int[] mill : MerelleBoard.MILLS) {
            int count = 0, freePos = -1;
            for (int p : mill) {
                MerellePawn pw = board.getPawnAt(p);
                if (pw != null && pw.getColor() == playerId) count++;
                else if (pw == null) freePos = p;
            }
            if (count == 2 && freePos >= 0) return freePos;
        }
        return -1;
    }

    /** Retourne tous les mouvements valides [src, dest] pour playerId. */
    private List<int[]> allPossibleMoves(MerelleBoard board, int playerId) {
        List<int[]> moves = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == playerId) {
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (board.isFreeAt(adj)) moves.add(new int[]{pos, adj});
            }
        }
        return moves;
    }

    /**
     * Retourne true si placer en pos bloquerait un moulin adverse presque complet
     * (2 pions opp dans un moulin contenant pos).
     */
    private boolean nearOpponentMill(MerelleBoard board, int pos, int opp) {
        for (int[] mill : MerelleBoard.MILLS) {
            boolean inMill = false;
            for (int p : mill) if (p == pos) { inMill = true; break; }
            if (!inMill) continue;
            int oppCount = 0;
            for (int p : mill) {
                MerellePawn pw = board.getPawnAt(p);
                if (pw != null && pw.getColor() == opp) oppCount++;
            }
            if (oppCount == 2) return true;
        }
        return false;
    }

    /** Retourne une position libre choisie aléatoirement. */
    private int randomFree(MerelleBoard board) {
        List<Integer> free = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++)
            if (board.isFreeAt(pos)) free.add(pos);
        if (free.isEmpty()) return -1;
        return free.get(random.nextInt(free.size()));
    }

    /**
     * Convertit une position logique (0-23) en coordonnée console (ex. "A1").
     * Lettre = ligne (A=0 … G=6), chiffre = colonne (1 … 7).
     */
    public static String posToCoord(int pos) {
        if (pos < 0 || pos >= 24) return "??";
        int row = MerelleBoard.POS_TO_GRID[pos][0];
        int col = MerelleBoard.POS_TO_GRID[pos][1];
        return "" + (char)('A' + row) + (col + 1);
    }
}
