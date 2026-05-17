package view;

import boardifier.model.GameException;
import boardifier.model.GameStageModel;
import boardifier.view.ClassicBoardLook;
import boardifier.view.GameStageView;
import boardifier.view.TextLook;
import model.MerelleStageModel;

/**
 * Vue console du jeu de la Mérelle.
 *
 * Crée les "looks" de chaque élément du stage grâce au framework boardifier.
 * Le rendu console est géré automatiquement par boardifier via View.update() → RootPane.
 *
 * Le plateau est affiché comme une grille 7x7 avec :
 *  - les coordonnées (colonnes A-G, lignes 1-7) affichées par ClassicBoardLook
 *  - les pions N (noir) et R (rouge) dans les cases correspondantes
 *
 * Les cases invalides du plateau apparaissent vides (espace).
 *
 * Calqué sur HoleStageView.java du tutoriel HoleConsole.
 */
public class MerelleStageView extends GameStageView {

    public MerelleStageView(String name, GameStageModel gameStageModel) {
        super(name, gameStageModel);
    }

    @Override
    public void createLooks() throws GameException {
        MerelleStageModel model = (MerelleStageModel) gameStageModel;

        // Look du texte affichant le nom du joueur courant
        addLook(new TextLook(model.getPlayerName()));

        // Look du plateau : grille 7x7, cellules 1x3, avec coordonnées
        // rowHeight=1, colWidth=3, depth=1, borderWidth=1, showCoords=true
        addLook(new ClassicBoardLook(1, 3, model.getBoard(), 1, 1, true));

        // Looks des 9 pions noirs
        for (MerellePawnLook look : createPawnLooks(model.getBlackPawns())) {
            addLook(look);
        }
        // Looks des 9 pions rouges
        for (MerellePawnLook look : createPawnLooks(model.getRedPawns())) {
            addLook(look);
        }
    }

    /**
     * Crée un tableau de MerellePawnLook pour un ensemble de pions.
     */
    private MerellePawnLook[] createPawnLooks(model.MerellePawn[] pawns) {
        MerellePawnLook[] looks = new MerellePawnLook[pawns.length];
        for (int i = 0; i < pawns.length; i++) {
            looks[i] = new MerellePawnLook(pawns[i]);
        }
        return looks;
    }
}
