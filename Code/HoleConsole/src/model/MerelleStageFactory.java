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
 * - 9 pions noirs (joueur 0) et 9 pions rouges (joueur 1)
 *
 * Les pions ne sont pas placés sur le plateau à la création : ils seront
 * posés un à un durant la phase 1.
 *
 * Calqué sur HoleStageFactory.java du tutoriel HoleConsole.
 */
public class MerelleStageFactory extends StageElementsFactory {

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
        // --- TextElement : nom du joueur courant ---
        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(0, 0);
        stageModel.setPlayerName(text);

        // --- Plateau de jeu en position (0,1) dans l'espace virtuel ---
        MerelleBoard board = new MerelleBoard(0, 1, stageModel);
        stageModel.setBoard(board);

        // --- 9 pions noirs (joueur 0) ---
        MerellePawn[] blackPawns = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            blackPawns[i] = new MerellePawn(MerellePawn.PAWN_BLACK, stageModel);
        }
        stageModel.setBlackPawns(blackPawns);

        // --- 9 pions rouges (joueur 1) ---
        MerellePawn[] redPawns = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            redPawns[i] = new MerellePawn(MerellePawn.PAWN_RED, stageModel);
        }
        stageModel.setRedPawns(redPawns);
    }
}
