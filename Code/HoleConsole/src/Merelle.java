import boardifier.control.Logger;
import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.MerelleController;

/**
 * Point d'entrée du jeu de la Mérelle.
 *
 * Usage : java Merelle [mode]
 *   mode 0 = Humain vs Humain  (défaut)
 *   mode 1 = Humain vs IA
 *   mode 2 = IA vs IA
 *
 * Lecture depuis un fichier d'entrée (section 1.4 des consignes) :
 *   java Merelle 0 < partie_normale.txt
 *
 * Syntaxe des ordres de jeu :
 *   Phase 1 (placement)   : "A1"      → place un pion en A1
 *   Phase 2 (déplacement) : "A1 B1"   → déplace de A1 vers B1
 *   Capture après moulin  : "XD4"     → capture le pion en D4
 *   Arrêt immédiat        : "stop"
 *
 * Système de coordonnées : lettre A-G (ligne), chiffre 1-7 (colonne).
 * A1 = coin haut-gauche, D4 = centre, G7 = coin bas-droite.
 *
 * Calqué sur HoleConsole.java du tutoriel.
 */
public class Merelle {

    public static void main(String[] args) {

        // Désactive les logs boardifier (trop verbeux en mode console)
        Logger.setLevel(Logger.LOGGER_NONE);

        // --- Lecture du mode de jeu ---
        int mode = 0;
        if (args.length >= 1) {
            try {
                mode = Integer.parseInt(args[0]);
                if (mode < 0 || mode > 2) {
                    System.err.println("Mode invalide. Valeurs valides : 0, 1 ou 2.");
                    System.err.println("  0 = Humain vs Humain");
                    System.err.println("  1 = Humain vs IA");
                    System.err.println("  2 = IA vs IA");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Mode invalide : '" + args[0] + "' (doit être 0, 1 ou 2).");
                System.exit(1);
            }
        }

        // --- Création du modèle et ajout des joueurs ---
        Model model = new Model();
        if (mode == 0) {
            System.out.println("Mode : Humain vs Humain");
            model.addHumanPlayer("Joueur N");
            model.addHumanPlayer("Joueur R");
        } else if (mode == 1) {
            System.out.println("Mode : Humain vs IA");
            model.addHumanPlayer("Joueur N");
            model.addComputerPlayer("Ordinateur R");
        } else {
            System.out.println("Mode : IA vs IA");
            model.addComputerPlayer("Ordi-N");
            model.addComputerPlayer("Ordi-R");
        }

        // --- Enregistrement du stage dans la factory ---
        StageFactory.registerModelAndView(
            "merelle",
            "model.MerelleStageModel",
            "view.MerelleStageView"
        );

        // --- Création de la vue et du contrôleur ---
        View view = new View(model);
        MerelleController control = new MerelleController(model, view);
        control.setFirstStageName("merelle");

        // --- Lancement ---
        System.out.println("=== JEU DE LA MERELLE ===");
        System.out.println("N = Noir, R = Rouge | Coordonnées : lettre A-G + chiffre 1-7 (ex: A1, D4)");
        System.out.println();
        try {
            control.startGame();
            control.stageLoop();
        } catch (GameException e) {
            System.out.println("Impossible de démarrer le jeu : " + e.getMessage());
        }
    }
}
