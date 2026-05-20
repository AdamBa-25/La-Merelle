package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MerelleDecider extends Decider {

    /** Constantes de difficulté */
    public static final int DIFFICULTY_MINIMAX    = 1;
    public static final int DIFFICULTY_ALPHABETA  = 2;
    public static final int DIFFICULTY_MONTECARLO = 3;

    /** Profondeur de recherche pour MiniMax et Alpha-Beta. */
    private static final int MINIMAX_DEPTH   = 5;
    private static final int ALPHABETA_DEPTH = 5;

    /** Nombre de simulations par coup pour Monte Carlo. */
    private static final int MCTS_SIMULATIONS = 50;

    /** Difficulté active, à définir avant le lancement de la partie. */
    public static int aiDifficulty = DIFFICULTY_MINIMAX;

    private static final Random random = new Random();

    /** Constructeur */
    public MerelleDecider(Model model, Controller control) {
        super(model, control);
    }

    @Override
    public ActionList decide() {
        return new ActionList();
    }

    /**
     * Calcule et retourne la décision de l'IA sous forme de chaîne,
     * dans le même format que la saisie clavier humaine.
     * Délègue à la stratégie choisie via aiDifficulty.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return la saisie simulée : "A1" (placement), "A1 B2" (déplacement), "XA1" (capture)
     */
    public String getDecision(MerelleStageModel stageModel, int playerId) {
        switch (aiDifficulty) {
            case DIFFICULTY_ALPHABETA:  return getDecisionAlphaBeta(stageModel, playerId);
            case DIFFICULTY_MONTECARLO: return getDecisionMonteCarlo(stageModel, playerId);
            default:                    return getDecisionMinimax(stageModel, playerId);
        }
    }

    // ================================================================
    // STRATÉGIE 1 : MINIMAX
    // ================================================================

    /**
     * Point d'entrée MiniMax.
     * Gère les 3 situations : capture, placement (phase 1), déplacement (phase 2).
     * Pour chaque coup possible, appelle minimax() et garde le meilleur score.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return la meilleure saisie trouvée par MiniMax
     */
    private String getDecisionMinimax(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();
        int phase = stageModel.getCurrentPhase();

        int colorAI  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int colorOpp = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        int[] snap = boardSnapshot(board, colorAI, colorOpp);

        // Dernier coup joué par CE joueur (2 entrées en arrière dans l'historique
        // partagé, car les deux joueurs alternent).
        // Format : "src->dest", ex. "9->10"
        String lastOwnMove = getLastOwnMove(stageModel, playerId);

        // --- CAS SPÉCIAL : capture ---
        if (stageModel.isMillJustFormed()) {
            List<Integer> captures = allCapturesSnap(snap, colorOpp);
            int bestScore = Integer.MIN_VALUE;
            int bestPos   = captures.get(0);
            for (int pos : captures) {
                int[] next = snapCopy(snap);
                next[pos] = -1;
                int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);
                if (score > bestScore) { bestScore = score; bestPos = pos; }
            }
            return "X" + posToCoord(bestPos);
        }

        // --- PHASE 1 : placement ---
        if (phase == MerelleStageModel.PHASE_PLACEMENT) {
            List<Integer> placements = allPlacementsSnap(snap);
            int bestScore = Integer.MIN_VALUE;
            int bestPos   = placements.get(0);
            for (int pos : placements) {
                int[] next = snapCopy(snap);
                next[pos] = colorAI;
                int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);
                if (score > bestScore) { bestScore = score; bestPos = pos; }
            }
            return posToCoord(bestPos);
        }

        // --- PHASE 2 : déplacement ---
        List<int[]> moves = allMovesSnap(snap, colorAI);
        int bestScore  = Integer.MIN_VALUE;
        int[] bestMove = moves.get(0);
        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;
            int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);

            // Pénalise le coup inverse du dernier coup de CE joueur (ping-pong).
            // Ex : si la dernière fois il a joué "9->10", jouer "10->9" = ping-pong.
            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) {
                score -= 5000;
            }

            if (score > bestScore) { bestScore = score; bestMove = mv; }
        }
        return posToCoord(bestMove[0]) + " " + posToCoord(bestMove[1]);
    }

    /**
     * Retourne le dernier coup joué par CE joueur (pas l'adversaire).
     * Dans un historique alterné [opp, me, opp], le coup du joueur courant
     * est à l'index 1 (2 coups en arrière).
     * Retourne null si pas encore joué.
     */
    private String getLastOwnMove(MerelleStageModel stageModel, int playerId) {
        String[] history = stageModel.getLastMoves();
        // Depuis la mise à jour du modèle, chaque entrée est préfixée par le joueur :
        // "0:9->10" ou "1:9->10". On cherche la dernière entrée appartenant à CE joueur.
        String prefix = playerId + ":";
        for (String entry : history) {
            if (entry != null && entry.startsWith(prefix)) {
                // On retire le préfixe "0:" ou "1:" pour ne garder que le coup brut
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    /**
     * Retourne true si moveStr est l'exact inverse de lastMove.
     * Ex : lastMove = "9->10", moveStr = "10->9" → true (ping-pong).
     * lastMove doit être sans préfixe joueur (déjà extrait par getLastOwnMove).
     */
    private boolean isPingPong(String lastMove, String moveStr) {
        if (lastMove == null || !lastMove.contains("->")) return false;
        String[] parts = lastMove.split("->");
        if (parts.length != 2) return false;
        String inverse = parts[1] + "->" + parts[0];
        return inverse.equals(moveStr);
    }

    /**
     * Algorithme MiniMax récursif travaillant sur un snapshot int[24].
     * Chaque case vaut : colorAI, colorOpp, ou -1 (libre).
     * Aucun objet boardifier n'est touché → pas d'événements, pas de débordement.
     *
     * @param snap         copie de l'état du plateau (int[24])
     * @param depth        profondeur restante
     * @param isMaximizing true = tour de l'IA, false = tour de l'adversaire
     * @param colorAI      constante couleur de l'IA
     * @param colorOpp     constante couleur de l'adversaire
     * @param phase        phase actuelle
     * @return score de l'état
     */
    private int minimax(int[] snap, int depth, boolean isMaximizing,
                        int colorAI, int colorOpp, int phase) {

        if (depth == 0 || isTerminalSnap(snap, colorAI, colorOpp, phase)) {
            return evaluateSnap(snap, colorAI, colorOpp);
        }

        int currentColor = isMaximizing ? colorAI : colorOpp;

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;
                    best = Math.max(best, minimax(next, depth - 1, false, colorAI, colorOpp, phase));
                }
            } else {
                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;
                    best = Math.max(best, minimax(next, depth - 1, false, colorAI, colorOpp, phase));
                }
            }
            return best == Integer.MIN_VALUE ? evaluateSnap(snap, colorAI, colorOpp) : best;

        } else {
            int best = Integer.MAX_VALUE;
            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;
                    best = Math.min(best, minimax(next, depth - 1, true, colorAI, colorOpp, phase));
                }
            } else {
                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;
                    best = Math.min(best, minimax(next, depth - 1, true, colorAI, colorOpp, phase));
                }
            }
            return best == Integer.MAX_VALUE ? evaluateSnap(snap, colorAI, colorOpp) : best;
        }
    }

    // ================================================================
    // STRATÉGIE 2 : ALPHA-BETA
    // ================================================================

    /**
     * Point d'entrée Alpha-Beta.
     * Identique à getDecisionMinimax() mais appelle alphabeta() au lieu de minimax().
     * Peut utiliser une profondeur plus grande (ALPHABETA_DEPTH) grâce à l'élagage.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return la meilleure saisie trouvée par Alpha-Beta
     */
    private String getDecisionAlphaBeta(MerelleStageModel stageModel, int playerId) {
        // TODO
        return null;
    }

    /**
     * Algorithme Alpha-Beta récursif (élagage du MiniMax).
     * Même logique que minimax() avec deux paramètres supplémentaires :
     * - alpha : meilleur score garanti pour le maximisant (IA)
     * - beta  : meilleur score garanti pour le minimisant (adversaire)
     * On élaguer une branche dès que alpha >= beta (inutile de continuer).
     *
     * @param board          état actuel du plateau
     * @param depth          profondeur restante
     * @param alpha          borne basse (score min garanti pour le maximisant)
     * @param beta           borne haute (score max garanti pour le minimisant)
     * @param isMaximizing   true si c'est le tour de l'IA
     * @param playerId       index du joueur IA (0 ou 1)
     * @param phase          phase actuelle : PHASE_PLACEMENT ou PHASE_DEPLACEMENT
     * @return score de l'état après élagage
     */
    private int alphabeta(MerelleBoard board, int depth, int alpha, int beta,
                          boolean isMaximizing, int playerId, int phase) {
        // TODO
        return 0;
    }

    // ================================================================
    // STRATÉGIE 3 : MONTE CARLO
    // ================================================================

    /**
     * Point d'entrée Monte Carlo.
     * Pour chaque coup possible, lance MCTS_SIMULATIONS parties aléatoires
     * depuis l'état résultant, et choisit le coup avec le meilleur taux de victoires.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return le coup avec le meilleur taux de victoires simulées
     */
    private String getDecisionMonteCarlo(MerelleStageModel stageModel, int playerId) {
        // TODO
        return null;
    }

    /**
     * Simule une partie complètement aléatoire depuis l'état actuel du plateau
     * jusqu'à ce qu'un état terminal soit atteint (victoire ou blocage).
     * À chaque tour, choisit un coup aléatoire parmi les coups valides.
     *
     * @param board        état actuel du plateau (copié avant la simulation)
     * @param currentPlayer index du joueur dont c'est le tour au début de la simulation
     * @param phase        phase actuelle au début de la simulation
     * @param colorJ1      couleur du joueur 0
     * @param colorJ2      couleur du joueur 1
     * @return index du joueur gagnant (0 ou 1), ou -1 si match nul / limite de tours
     */
    private int simulateRandomGame(MerelleBoard board, int currentPlayer, int phase,
                                   int colorJ1, int colorJ2) {
        // TODO
        return -1;
    }

    // ================================================================
    // ÉVALUATION
    // ================================================================

    /**
     * Fonction d'évaluation heuristique d'un état du plateau.
     * Retourne un score du point de vue de playerId :
     *   score positif  -> position favorable à l'IA
     *   score négatif  -> position favorable à l'adversaire
     *   ±10000         -> victoire / défaite (état terminal)
     *
     * Critères pris en compte :
     *   - nombre de pions sur le plateau (×10)
     *   - nombre de moulins formés (×50)
     *   - mobilité : nombre de coups disponibles (×2)
     *
     * @param board    état du plateau à évaluer
     * @param colorAI  couleur du joueur IA (constante MerellePawn.PAWN_*)
     * @param colorOpp couleur de l'adversaire
     * @return score entier centré sur 0
     */
    private int evaluate(MerelleBoard board, int colorAI, int colorOpp) {
        int[] snap = boardSnapshot(board, colorAI, colorOpp);
        return evaluateSnap(snap, colorAI, colorOpp);
    }

    // ================================================================
    // SNAPSHOT — représentation légère du plateau en int[24]
    // ================================================================

    /**
     * Convertit le board réel en tableau int[24].
     * Chaque case vaut : colorAI, colorOpp, ou -1 (libre).
     * Aucun objet boardifier n'est créé ni modifié.
     */
    private int[] boardSnapshot(MerelleBoard board, int colorAI, int colorOpp) {
        int[] snap = new int[24];
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw == null)                    snap[pos] = -1;
            else if (pw.getColor() == colorAI) snap[pos] = colorAI;
            else                               snap[pos] = colorOpp;
        }
        return snap;
    }

    /** Copie un snapshot (pour ne pas modifier l'original lors de la simulation). */
    private int[] snapCopy(int[] snap) {
        return snap.clone();
    }

    /** Évaluation heuristique sur un snapshot int[24]. */
    private int evaluateSnap(int[] snap, int colorAI, int colorOpp) {

        // ── 1. Comptage des pions ───────────────────────────────────────────
        int pawnsAI = 0, pawnsOpp = 0;
        for (int v : snap) {
            if (v == colorAI)  pawnsAI++;
            if (v == colorOpp) pawnsOpp++;
        }

        // ── 2. États terminaux (priorité absolue, jamais dépassé) ───────────
        if (pawnsOpp < 3) return +10000;
        if (pawnsAI  < 3) return -10000;

        // ── 3. Mobilité brute ───────────────────────────────────────────────
        // Un joueur sans coup disponible perd immédiatement → valeur terminale.
        List<int[]> movesAI  = allMovesSnap(snap, colorAI);
        List<int[]> movesOpp = allMovesSnap(snap, colorOpp);
        if (movesAI.isEmpty())  return -10000;
        if (movesOpp.isEmpty()) return +10000;
        int mobilityAI  = movesAI.size();
        int mobilityOpp = movesOpp.size();

        // ── 4. Analyse de tous les moulins en un seul passage ───────────────
        // Pour chaque moulin, on compte les pions AI/Opp et les cases libres.
        // Cela permet de calculer d'un coup : moulins complets, quasi-moulins,
        // moulins bloqués par l'adversaire, moulins ouverts.
        int millsAI       = 0, millsOpp       = 0; // moulins complets
        int nearAI        = 0, nearOpp        = 0; // 2 pions + 1 case libre (menace directe)
        int openOneAI     = 0, openOneOpp     = 0; // 1 pion + 2 cases libres (développement)
        int blockedNearAI = 0, blockedNearOpp = 0; // 2 pions AI bloqués par 1 opp (et vice-versa)
        int inMillAI      = 0, inMillOpp      = 0; // pions actuellement dans un moulin complet

        for (int[] mill : MerelleBoard.MILLS) {
            int cntAI = 0, cntOpp = 0, free = 0;
            for (int pos : mill) {
                if      (snap[pos] == colorAI)  cntAI++;
                else if (snap[pos] == colorOpp) cntOpp++;
                else                            free++;
            }

            if (cntAI  == 3) { millsAI++;  inMillAI  += 3; }
            if (cntOpp == 3) { millsOpp++; inMillOpp += 3; }

            if (cntAI  == 2 && free == 1) nearAI++;          // menace de moulin AI
            if (cntOpp == 2 && free == 1) nearOpp++;          // menace de moulin Opp
            if (cntAI  == 2 && cntOpp == 1) blockedNearAI++; // quasi-moulin AI bloqué
            if (cntOpp == 2 && cntAI  == 1) blockedNearOpp++;// quasi-moulin Opp bloqué
            if (cntAI  == 1 && free == 2) openOneAI++;        // case ouverte AI
            if (cntOpp == 1 && free == 2) openOneOpp++;       // case ouverte Opp
        }

        // ── 5. Analyse pion par pion ─────────────────────────────────────────
        // Pour chaque pion, on regarde : est-il coincé ? combien de voisins libres ?
        // Position stratégique (connectivité = nb de moulins potentiels par case).
        // Les cases "coin" de la Mérelle (B4, D2, D6, F4 = cases à 4 adjacences)
        // sont les plus précieuses car elles participent à plus de moulins.
        int stuckAI  = 0, stuckOpp  = 0;   // pions sans aucune case adjacente libre
        int freedomAI = 0, freedomOpp = 0; // somme des cases libres adjacentes
        int connectAI = 0, connectOpp = 0; // somme des moulins potentiels par case

        // Table précalculée : nombre de moulins auxquels appartient chaque case.
        // Calculé une seule fois ici pour éviter de reboucler sur MILLS.
        int[] millMembership = new int[24];
        for (int[] mill : MerelleBoard.MILLS)
            for (int pos : mill) millMembership[pos]++;

        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == colorAI) {
                int freeDeg = 0;
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (snap[adj] == -1) freeDeg++;
                if (freeDeg == 0) stuckAI++;
                freedomAI += freeDeg;
                connectAI += millMembership[pos];
            } else if (snap[pos] == colorOpp) {
                int freeDeg = 0;
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (snap[adj] == -1) freeDeg++;
                if (freeDeg == 0) stuckOpp++;
                freedomOpp += freeDeg;
                connectOpp += millMembership[pos];
            }
        }

        // ── 6. Pions hors moulin (vulnérables à la capture) ─────────────────
        // Un pion hors moulin est prenable. Moins on en a, mieux c'est.
        // inMillAI/Opp comptait les pions dans un moulin (avec doublons si 2 moulins),
        // on cap à pawnsAI/Opp pour éviter les sur-comptages.
        int exposedAI  = pawnsAI  - Math.min(inMillAI,  pawnsAI);
        int exposedOpp = pawnsOpp - Math.min(inMillOpp, pawnsOpp);

        // ── 7. Score final pondéré ───────────────────────────────────────────
        // Chaque critère est pondéré selon son impact réel sur l'issue de la partie.
        // Hiérarchie : moulins > menaces > pions > mobilité > position > détails
        return
                // Avantage numérique : chaque pion supplémentaire vaut 10
                (pawnsAI - pawnsOpp)           * 10

                        // Moulins complets : très fort, donne une capture immédiate
                        + (millsAI - millsOpp)           * 50

                        // Menaces directes : 2 pions alignés + 1 case libre → moulin au prochain coup
                        + (nearAI - nearOpp)             * 30

                        // Quasi-moulins bloqués par l'adversaire : mauvais pour celui qui est bloqué
                        + (blockedNearOpp - blockedNearAI) * 15

                        // Mobilité globale : plus de coups = plus d'options
                        + (mobilityAI - mobilityOpp)     * 5

                        // Liberté par pion : cases libres adjacentes = manœuvrabilité
                        + (freedomAI - freedomOpp)       * 3

                        // Pions coincés (aucune case adjacente libre) : très mauvais
                        + (stuckOpp - stuckAI)           * 12

                        // Pions exposés (hors moulin) : ils peuvent être capturés
                        + (exposedOpp - exposedAI)       * 8

                        // Connectivité stratégique : cases qui participent à plus de moulins
                        + (connectAI - connectOpp)       * 2

                        // Cases ouvertes : développement, potentiel futur
                        + (openOneAI - openOneOpp)       * 4;
    }

    /** isTerminal sur un snapshot. */
    private boolean isTerminalSnap(int[] snap, int colorAI, int colorOpp, int phase) {
        int pawnsAI = 0, pawnsOpp = 0;
        for (int v : snap) {
            if (v == colorAI)  pawnsAI++;
            if (v == colorOpp) pawnsOpp++;
        }
        if (pawnsAI  < 3) return true;
        if (pawnsOpp < 3) return true;
        if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
            if (allMovesSnap(snap, colorAI).isEmpty())  return true;
            if (allMovesSnap(snap, colorOpp).isEmpty()) return true;
        }
        return false;
    }

    /** Positions libres sur un snapshot. */
    private List<Integer> allPlacementsSnap(int[] snap) {
        List<Integer> result = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++)
            if (snap[pos] == -1) result.add(pos);
        return result;
    }

    /** Déplacements valides [src, dest] pour une couleur sur un snapshot. */
    private List<int[]> allMovesSnap(int[] snap, int color) {
        List<int[]> moves = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == color) {
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (snap[adj] == -1) moves.add(new int[]{pos, adj});
            }
        }
        return moves;
    }

    /** Captures disponibles pour oppColor sur un snapshot. */
    private List<Integer> allCapturesSnap(int[] snap, int oppColor) {
        // Vérifie si tous les pions adverses sont en moulin
        boolean allInMills = true;
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == oppColor && !isInMillSnap(snap, pos, oppColor)) {
                allInMills = false;
                break;
            }
        }
        List<Integer> targets = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == oppColor) {
                if (allInMills || !isInMillSnap(snap, pos, oppColor))
                    targets.add(pos);
            }
        }
        return targets;
    }

    /** Vérifie si une position est dans un moulin complet sur un snapshot. */
    private boolean isInMillSnap(int[] snap, int pos, int color) {
        for (int[] mill : MerelleBoard.MILLS) {
            boolean inMill = false;
            for (int p : mill) if (p == pos) { inMill = true; break; }
            if (!inMill) continue;
            boolean full = true;
            for (int p : mill) if (snap[p] != color) { full = false; break; }
            if (full) return true;
        }
        return false;
    }

    // ================================================================
    // UTILITAIRES — GÉNÉRATION DES COUPS
    // ================================================================

    /**
     * Retourne toutes les positions libres où un joueur peut placer un pion (phase 1).
     *
     * @param board état du plateau
     * @return liste des positions libres (0-23)
     */
    private List<Integer> allPlacements(MerelleBoard board) {
        List<Integer> result = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++)
            if (board.isFreeAt(pos)) result.add(pos);
        return result;
    }

    /**
     * Retourne tous les déplacements valides [src, dest] pour la couleur donnée (phase 2).
     * Un déplacement est valide si src contient un pion de la couleur et dest est libre et adjacent.
     *
     * @param board       état du plateau
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     * @return liste de tableaux [src, dest]
     */
    private List<int[]> allMoves(MerelleBoard board, int playerColor) {
        List<int[]> moves = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (board.isFreeAt(adj)) moves.add(new int[]{pos, adj});
            }
        }
        return moves;
    }

    /**
     * Retourne toutes les positions adverses capturables.
     * Si tous les pions adverses sont en moulin, tous sont capturables (règle officielle).
     * Sinon, seuls les pions hors moulin sont capturables.
     *
     * @param board    état du plateau
     * @param oppColor couleur de l'adversaire (constante MerellePawn.PAWN_*)
     * @return liste des positions adverses capturables
     */
    private List<Integer> allCaptures(MerelleBoard board, int oppColor) {
        boolean allInMills = board.allPawnsInMills(oppColor);
        List<Integer> targets = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == oppColor) {
                if (allInMills || !board.isInMill(pos, oppColor))
                    targets.add(pos);
            }
        }
        return targets;
    }

    // ================================================================
    // UTILITAIRES — ÉTAT TERMINAL
    // ================================================================

    /**
     * Retourne true si l'état du plateau est terminal (la partie est finie).
     * Conditions de fin : un joueur a moins de 3 pions sur le plateau,
     * ou un joueur ne peut plus bouger (bloqué) en phase 2.
     *
     * @param board    état du plateau
     * @param colorAI  couleur du joueur IA
     * @param colorOpp couleur de l'adversaire
     * @param phase    phase actuelle (PHASE_PLACEMENT ou PHASE_DEPLACEMENT)
     * @return true si la partie est terminée
     */
    private boolean isTerminal(MerelleBoard board, int colorAI, int colorOpp, int phase) {
        if (board.countPawns(colorAI)  < 3) return true;
        if (board.countPawns(colorOpp) < 3) return true;
        if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
            if (board.isBlocked(colorAI))  return true;
            if (board.isBlocked(colorOpp)) return true;
        }
        return false;
    }

    // ================================================================
    // UTILITAIRES — CONVERSION
    // ================================================================

    /**
     * Convertit une position logique (0-23) en coordonnée console (ex. "A1").
     * Lettre = ligne (A=0 à G=6), chiffre = colonne (1 à 7).
     *
     * @param pos position logique (0-23)
     * @return chaîne de 2 caractères, ex. "D4", ou "??" si pos invalide
     */
    public static String posToCoord(int pos) {
        if (pos < 0 || pos >= 24) return "??";
        int row = MerelleBoard.POS_TO_GRID[pos][0];
        int col = MerelleBoard.POS_TO_GRID[pos][1];
        return "" + (char)('A' + row) + (col + 1);
    }
}