package model;

import boardifier.model.GameStageModel;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Factory du stage de la Mérelle.
 *
 * Crée et enregistre dans le stage tous les éléments du jeu :
 * - le plateau (MerelleBoard) en position (0,1) dans l'espace virtuel
 * - un TextElement pour le nom du joueur courant en (0,0)
 * - 9 pions joueur 0 et 9 pions joueur 1 avec les couleurs choisies
 *
 * Les couleurs sont transmises via colorJ1 / colorJ2 avant le lancement.
 * Les pions ne sont pas placés sur le plateau à la création.
 *
 * Calqué sur HoleStageFactory.java du tutoriel HoleConsole.
 */
public class MerelleStageFactory extends StageElementsFactory {

    /**
     * Couleurs choisies par les joueurs, assignées depuis Merelle.java
     * avant que le stage soit créé.
     * Valeurs par défaut : Noir pour J1, Rouge pour J2.
     */
    public static int colorJ1 = MerellePawn.PAWN_BLACK;
    public static int colorJ2 = MerellePawn.PAWN_RED;

    private MerelleStageModel stageModel;

    /**
     * @param gameStageModel le stage à initialiser (cast en MerelleStageModel)
     */
    public MerelleStageFactory(GameStageModel gameStageModel) {
        super(gameStageModel);
        stageModel = (MerelleStageModel) gameStageModel;
    }

    /**
     * Crée tous les éléments du jeu et les assigne au stage model.
     * Cette méthode est appelée automatiquement par GameStageModel.createElements().
     */
    @Override
    public void setup() {

        // Sécurité : si une couleur invalide a été transmise, on remet les valeurs par défaut
        if (!MerellePawn.isValidColor(colorJ1)) colorJ1 = MerellePawn.PAWN_BLACK;
        if (!MerellePawn.isValidColor(colorJ2)) colorJ2 = MerellePawn.PAWN_RED;
        if (colorJ1 == colorJ2) colorJ2 = MerellePawn.PAWN_RED;

        // --- TextElement : nom du joueur courant ---
        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(0, 0);
        stageModel.setPlayerName(text);

        // --- Plateau de jeu en position (0,1) dans l'espace virtuel ---
        MerelleBoard board = new MerelleBoard(0, 1, stageModel);
        stageModel.setBoard(board);

        // --- 9 pions joueur 0 avec la couleur choisie ---
        MerellePawn[] pawnsJ1 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ1[i] = new MerellePawn(colorJ1, stageModel);
        }
        stageModel.setBlackPawns(pawnsJ1);

        // --- 9 pions joueur 1 avec la couleur choisie ---
        MerellePawn[] pawnsJ2 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ2[i] = new MerellePawn(colorJ2, stageModel);
        }
        stageModel.setRedPawns(pawnsJ2);
    }
}