package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Contrôleur principal du jeu de la Mérelle.
 *
 * Gère la boucle de jeu, la saisie clavier (ou fichier), la validation des coups
 * et l'application des règles. Hérite de Controller comme HoleController.
 *
 * Syntaxe des ordres de jeu :
 *  - Phase 1 (placement)   : "A1"      → place un pion en ligne A, colonne 1
 *  - Phase 2 (déplacement) : "A1 B1"   → déplace de A1 vers B1
 *  - Capture après moulin  : "XA1"     → capture le pion adverse en A1
 *  - Arrêt immédiat        : toute saisie contenant "stop"
 *
 * Système de coordonnées : lettre A-G (ligne, de haut en bas),
 *                          chiffre 1-7 (colonne, de gauche à droite).
 *
 * Calqué sur HoleController.java du tutoriel HoleConsole.
 */
public class MerelleController extends Controller {

    private BufferedReader consoleIn;

    public MerelleController(Model model, View view) {
        super(model, view);
    }

    /**
     * Boucle principale d'une partie.
     * Affiche, lit les coups et alterne les joueurs jusqu'à la fin de la partie.
     */
    @Override
    public void stageLoop() {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        // Premier affichage
        update();
        while (!model.isEndStage()) {
            playTurn();
            endOfTurn();
            update();
        }
        endGame();
    }

    /**
     * Joue un tour pour le joueur courant (humain ou IA).
     */
    private void playTurn() {
        Player p = model.getCurrentPlayer();
        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();

        if (p.getType() == Player.COMPUTER) {
            // Joueur IA : demande la décision au décideur
            System.out.println(p.getName() + " (IA) réfléchit...");
            MerelleDecider decider = new MerelleDecider(model, this);
            String decision = decider.getDecision(stageModel, model.getIdPlayer());
            System.out.println(p.getName() + " joue : " + decision);
            boolean ok = analyseAndPlay(decision);
            if (!ok) {
                // Ne devrait pas arriver avec une IA correcte
                System.out.println("ERREUR IA : coup invalide généré (" + decision + ")");
            }
        } else {
            // Joueur humain : boucle jusqu'à un coup valide
            boolean ok = false;
            while (!ok) {
                System.out.print(p.getName() + " > ");
                try {
                    String line = consoleIn.readLine();
                    // Fin de fichier (EOF) → arrêt propre
                    if (line == null) { System.exit(0); }
                    line = line.trim();
                    // Arrêt immédiat si "stop" détecté
                    if (line.toLowerCase().contains("stop")) {
                        System.out.println("Arrêt demandé. Fin du jeu.");
                        System.exit(0);
                    }
                    ok = analyseAndPlay(line);
                    if (!ok) {
                        System.out.println("Ordre invalide, recommencez !");
                    }
                } catch (IOException e) {
                    System.err.println("Erreur de lecture : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Passe au joueur suivant et met à jour l'affichage du nom.
     */
    @Override
    public void endOfTurn() {
        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();

        // Si une capture est en attente, le joueur courant rejoue pour capturer
        if (stageModel.isMillJustFormed()) return;

        model.setNextPlayer();
        stageModel.getPlayerName().setText(model.getCurrentPlayerName());

        // Vérifie la transition phase 1 → phase 2
        if (stageModel.checkAndTransitionToMovePhase()) {
            System.out.println(">>> Tous les pions sont posés ! Début de la phase de déplacement. <<<");
        }

        // Vérifie les conditions de fin après le changement de joueur
        checkEndConditions(stageModel);
    }

    /**
     * Analyse la saisie et exécute le coup si valide.
     *
     * Trois cas :
     *  1. Capture (commence par 'X') : retire un pion adverse
     *  2. Phase 1 – une coordonnée : place un pion
     *  3. Phase 2 – deux coordonnées séparées par espace : déplace un pion
     *
     * Affiche un message d'erreur précis en cas de saisie invalide.
     *
     * @param input  saisie brute du joueur
     * @return true si le coup est valide et a été exécuté
     */
    private boolean analyseAndPlay(String input) {
        if (input == null || input.isEmpty()) {
            System.out.println("ERREUR [SYNTAXE] : saisie vide.");
            return false;
        }

        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();
        MerelleBoard board = stageModel.getBoard();
        int playerId = model.getIdPlayer();

        // ============================================================
        // CAS 1 : Capture obligatoire après formation d'un moulin
        // ============================================================
        if (stageModel.isMillJustFormed()) {
            if (!input.toUpperCase().startsWith("X")) {
                System.out.println("ERREUR [RÈGLES] : vous devez capturer un pion adverse !");
                System.out.println("  Format : X suivi de la coordonnée (ex: XA1)");
                return false;
            }
            String coord = input.substring(1).trim();
            int pos = parseCoord(coord);
            if (pos < 0) {
                System.out.println("ERREUR [SYNTAXE] : coordonnée invalide '" + coord + "' (ex: XA1, XD4).");
                return false;
            }
            MerellePawn target = board.getPawnAt(pos);
            if (target == null) {
                System.out.println("ERREUR [RÈGLES] : aucun pion à capturer en " + coord.toUpperCase() + ".");
                return false;
            }
            if (target.getColor() == playerId) {
                System.out.println("ERREUR [RÈGLES] : vous ne pouvez pas capturer votre propre pion !");
                return false;
            }
            int opp = 1 - playerId;
            // Un pion en moulin est protégé, sauf si tous les adversaires sont en moulin
            if (board.isInMill(pos, opp) && !board.allPawnsInMills(opp)) {
                System.out.println("ERREUR [RÈGLES] : ce pion est protégé par un moulin !");
                System.out.println("  Choisissez un pion hors moulin.");
                return false;
            }
            // Exécute la capture via ActionFactory (comme le tutoriel)
            ActionList actions = ActionFactory.generateRemoveFromContainer(model, target);
            ActionPlayer play = new ActionPlayer(model, this, actions);
            play.start();
            stageModel.setMillJustFormed(false);
            stageModel.recordMove("capture:" + pos);
            System.out.println("  → " + model.getCurrentPlayerName() + " capture le pion en " + coord.toUpperCase() + " !");
            // Vérifie si l'adversaire a perdu
            checkEndConditions(stageModel);
            return true;
        }

        // ============================================================
        // CAS 2 : Phase de placement (une seule coordonnée)
        // ============================================================
        if (stageModel.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) {
            if (input.contains(" ")) {
                System.out.println("ERREUR [SYNTAXE] : en phase de placement, entrez une seule coordonnée (ex: A1).");
                return false;
            }
            int pos = parseCoord(input);
            if (pos < 0) {
                System.out.println("ERREUR [SYNTAXE] : coordonnée invalide '" + input + "' (lettre A-G + chiffre 1-7, ex: A1).");
                return false;
            }
            if (!board.isFreeAt(pos)) {
                System.out.println("ERREUR [RÈGLES] : la case " + input.toUpperCase() + " est déjà occupée !");
                return false;
            }
            if (stageModel.getPawnsInHand(playerId) <= 0) {
                System.out.println("ERREUR [RÈGLES] : vous n'avez plus de pions à placer !");
                return false;
            }

            // Récupère le premier pion encore hors plateau
            MerellePawn pawn = getNextPawnInHand(stageModel, playerId);
            if (pawn == null) {
                System.out.println("ERREUR INTERNE : aucun pion disponible.");
                return false;
            }

            // Place le pion via ActionFactory
            ActionList actions = ActionFactory.generatePutInContainer(
                    model, pawn, "merelleboard",
                    MerelleBoard.POS_TO_GRID[pos][0],
                    MerelleBoard.POS_TO_GRID[pos][1]);
            ActionPlayer play = new ActionPlayer(model, this, actions);
            play.start();

            stageModel.decreasePawnsInHand(playerId);
            stageModel.recordMove("place:" + pos);
            System.out.println("  → " + model.getCurrentPlayerName() + " place un pion en " + input.toUpperCase());

            // Détecte la formation d'un moulin
            if (board.checkMillFormed(pos, playerId)) {
                stageModel.setMillJustFormed(true);
                System.out.println("  >>> MOULIN formé ! Capturez un pion adverse (ex: XA1) <<<");
            }
            return true;
        }

        // ============================================================
        // CAS 3 : Phase de déplacement (deux coordonnées)
        // ============================================================
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("ERREUR [SYNTAXE] : en phase de déplacement, entrez source et destination (ex: A1 B1).");
            return false;
        }
        int src  = parseCoord(parts[0]);
        int dest = parseCoord(parts[1]);
        if (src < 0) {
            System.out.println("ERREUR [SYNTAXE] : coordonnée source invalide '" + parts[0] + "'.");
            return false;
        }
        if (dest < 0) {
            System.out.println("ERREUR [SYNTAXE] : coordonnée destination invalide '" + parts[1] + "'.");
            return false;
        }
        MerellePawn pawn = board.getPawnAt(src);
        if (pawn == null) {
            System.out.println("ERREUR [RÈGLES] : aucun pion en " + parts[0].toUpperCase() + ".");
            return false;
        }
        if (pawn.getColor() != playerId) {
            System.out.println("ERREUR [RÈGLES] : ce pion ne vous appartient pas !");
            return false;
        }
        if (!board.isFreeAt(dest)) {
            System.out.println("ERREUR [RÈGLES] : la case " + parts[1].toUpperCase() + " est occupée !");
            return false;
        }
        if (!board.isAdjacent(src, dest)) {
            System.out.println("ERREUR [RÈGLES] : " + parts[0].toUpperCase() + " et " + parts[1].toUpperCase()
                    + " ne sont pas adjacents sur le plateau !");
            return false;
        }

        // Déplace le pion via ActionFactory (MoveWithinContainer)
        ActionList actions = ActionFactory.generateMoveWithinContainer(
                model, pawn,
                MerelleBoard.POS_TO_GRID[dest][0],
                MerelleBoard.POS_TO_GRID[dest][1]);
        ActionPlayer play = new ActionPlayer(model, this, actions);
        play.start();

        stageModel.recordMove(src + "->" + dest);
        System.out.println("  → " + model.getCurrentPlayerName()
                + " déplace " + parts[0].toUpperCase() + " → " + parts[1].toUpperCase());

        // Détecte la formation d'un moulin après le déplacement
        if (board.checkMillFormed(dest, playerId)) {
            stageModel.setMillJustFormed(true);
            System.out.println("  >>> MOULIN formé ! Capturez un pion adverse (ex: XA1) <<<");
        }
        return true;
    }

    /**
     * Vérifie les conditions de fin de partie et les applique au modèle.
     * Appelée après chaque coup et après chaque changement de joueur.
     *
     * Conditions de victoire (phase 2 uniquement) :
     *  - Un joueur a moins de 3 pions sur le plateau
     *  - Un joueur est bloqué (aucun déplacement possible)
     *
     * Condition d'égalité :
     *  - Répétition des 3 derniers coups identiques
     */
    private void checkEndConditions(MerelleStageModel stageModel) {
        // Pas de vérification en phase 1 ni si une capture est en attente
        if (stageModel.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) return;
        if (stageModel.isMillJustFormed()) return;

        MerelleBoard board = stageModel.getBoard();
        for (int pid = 0; pid < 2; pid++) {
            int count = board.countPawns(pid);
            // Défaite : moins de 3 pions
            if (count < 3) {
                System.out.println(model.getPlayers().get(pid).getName()
                        + " n'a plus que " + count + " pion(s) !");
                model.setIdWinner(1 - pid);
                model.stopStage();
                return;
            }
            // Défaite : bloqué
            if (board.isBlocked(pid)) {
                System.out.println(model.getPlayers().get(pid).getName()
                        + " est bloqué, aucun déplacement possible !");
                model.setIdWinner(1 - pid);
                model.stopStage();
                return;
            }
        }
        // Égalité par répétition
        if (stageModel.isDrawByRepetition()) {
            System.out.println("Égalité par répétition de coups !");
            model.setIdWinner(-1);
            model.stopStage();
        }
    }

    /**
     * Affiche le résultat de la partie.
     */
    @Override
    public void endGame() {
        System.out.println();
        System.out.println("=== FIN DE PARTIE ===");
        if (model.getIdWinner() >= 0) {
            System.out.println(">>> " + model.getPlayers().get(model.getIdWinner()).getName() + " gagne ! <<<");
        } else {
            System.out.println(">>> Match nul ! <<<");
        }
    }

    /**
     * Convertit une coordonnée textuelle en position logique (0-23).
     * Format : lettre A-G (ligne) + chiffre 1-7 (colonne), ex. "A1", "d4".
     *
     * @param coord la chaîne à analyser
     * @return position logique (0-23), ou -1 si invalide
     */
    public static int parseCoord(String coord) {
        if (coord == null || coord.length() < 2) return -1;
        coord = coord.toUpperCase().trim();
        char rowChar = coord.charAt(0);
        if (rowChar < 'A' || rowChar > 'G') return -1;
        int row = rowChar - 'A';
        int col;
        try {
            col = Integer.parseInt(coord.substring(1)) - 1; // "1"→0, "7"→6
        } catch (NumberFormatException e) { return -1; }
        if (col < 0 || col > 6) return -1;
        // Cherche la position logique correspondant à (row, col)
        for (int pos = 0; pos < 24; pos++) {
            if (MerelleBoard.POS_TO_GRID[pos][0] == row
                    && MerelleBoard.POS_TO_GRID[pos][1] == col)
                return pos;
        }
        return -1; // case de la grille mais pas une position valide du plateau
    }

    /**
     * Retourne le premier pion en main (pas encore placé sur le plateau)
     * pour le joueur donné. Un pion "en main" n'a pas de container.
     */
    private MerellePawn getNextPawnInHand(MerelleStageModel stageModel, int playerId) {
        MerellePawn[] pawns = (playerId == 0)
                ? stageModel.getPawnsJ1()
                : stageModel.getPawnsJ2();
        for (MerellePawn pawn : pawns)
            if (pawn.getContainer() == null) return pawn;
        return null;
    }
}
