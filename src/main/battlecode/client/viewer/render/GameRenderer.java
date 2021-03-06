package battlecode.client.viewer.render;

import battlecode.client.viewer.*;
import battlecode.common.*;
import battlecode.serial.*;
import battlecode.client.util.ImageFile;
import battlecode.server.Config;
import battlecode.world.GameMap;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class GameRenderer extends BaseRenderer {

    private BufferedMatch match;
    private GameStateTimeline<DrawState> timeline;
    private DrawCutScene cutScene;
    private DrawHUD sideA, sideB;
    private DrawMap drawMap;
    private DrawState ds;
    private MapLocation origin;
    private int maxRounds = 0;
    private DebugState debugState;
    private final Color winnerMask = new Color(0, 0, 0, 0.6f);
    private Font debugFont;
    private float spriteSize = RenderConfiguration.getInstance().getSpriteSize();
    private float unitWidth, unitHeight; // size of gc in sprite [grid] units
    private float unitHUDwidth;
    private float unitOffX, unitOffY;
    private Dimension canvasSize = null;
    private final Rectangle2D.Float clipRect = new Rectangle2D.Float();
    private AffineTransform hudScale;
    private ImageFile teamA, teamB, winnerImage;
    private FramerateTracker fps = new FramerateTracker(30);
    private boolean fastForward = false;
    private int targetID = -1;
    static private boolean loadedPrefsAlready = false;
    private final MatchListener ml = new MatchListener() {

        public void headerReceived(BufferedMatch m) {
            processHeader(m.getHeader());
        }

        public void breakReceived(BufferedMatch m) {
            setDebugEnabled(true);
        }

        public void footerReceived(BufferedMatch m) {
            processFooter(m.getFooter());
        }
    };
    private Runnable matchStarter = null;

    public GameRenderer() {
    }

    public GameRenderer(BufferedMatch match) {
        this.match = match;
        debugFont = new Font(null, Font.PLAIN, 2);
        ds = new DrawState();
        try {
            sideA = new DrawHUD(ds, Team.A, match.getTeamA());
            sideB = new DrawHUD(ds, Team.B, match.getTeamB());
        } catch (Error e) {
            e.printStackTrace();
        }

        timeline = new GameStateTimeline<DrawState>(match, DrawState.FACTORY, 10);
        timeline.setTargetState(ds);
        match.addMatchListener(ml);
        match.addMatchPausedListener(ml);
        loadPrefs();
    }
    // wins obtained by each side
    int aWins = 0, bWins = 0;
    // methods for win display

    public void resetMatches() {
        aWins = 0;
        bWins = 0;
    }

    public void addWin(Team t) {
        if (t == Team.A) {
            aWins++;
        } else if (t == Team.B) {
            bWins++;
        }
    }

    public void loadPrefs() {
        if (!loadedPrefsAlready) {
            for (char ch : Config.getGlobalConfig().get("bc.client.renderprefs2d").toCharArray()) {
                handleAction(ch);
            }
            loadedPrefsAlready = true;
        }
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#getTimeline()
     */
    public GameStateTimeline getTimeline() {
        return timeline;
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#setMatchStarter(java.lang.Runnable)
     */
    public void setMatchStarter(Runnable starter) {
        if (cutScene != null && cutScene.step == DrawCutScene.Step.GAME) {
            starter.run();
        } else {
            matchStarter = starter;
        }
    }

    private void setDebugEnabled(boolean enabled) {
        if (debugState != null) {
            debugState.setEnabled(enabled);
        }
    }

    protected boolean trySkipRounds(int rounds) {
        int targetRound = timeline.getRound() + rounds;
        if (targetRound < timeline.getNumRounds()) {
            timeline.setRound(targetRound);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private synchronized void processHeader(MatchHeader header) {
        GameMap map = (GameMap) header.getMap();
        drawMap = new DrawMap(map);
        origin = map.getMapOrigin();
        sideA.setFooterText("GAME " + (header.getMatchNumber() + 1));
        maxRounds = map.getMaxRounds();

        unitHeight = drawMap.getMapHeight();
        unitHUDwidth = sideA.getRatioWidth() * unitHeight;
        unitWidth = drawMap.getMapWidth() + 2 * unitHUDwidth;
        clipRect.width = drawMap.getMapWidth();
        clipRect.height = drawMap.getMapHeight();
        hudScale = AffineTransform.getScaleInstance(unitHeight, unitHeight);

        if (RenderConfiguration.getInstance().isTournamentMode()) {
            (new Thread() {

                public void run() {
                    while (match.getTeamA() == null) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                    cutScene = new DrawCutScene(unitWidth, unitHeight,
                            match.getTeamA(), match.getTeamB());
                    //FIXME: commented out for now
//					try {
//						String path = "map-backgrounds/" + match.getMapNames()[match.getHeader().getMatchNumber()] + ".xml.png";
//						System.out.println("loading " + path);
//						ImageFile imgFile = new ImageFile(path);
//						drawMap.prerenderMap(imgFile.image);
//						imgFile.unload();
//					}
//					catch (NullPointerException e) {
//						e.printStackTrace();
//						//drawMap.prerenderMap();
//					}
                }
            }).start();
        }
    }

    private void processFooter(MatchFooter footer) {
        if (cutScene != null) {
            cutScene.setWinner(footer.getWinner());
        }
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#setCanvasSize(java.awt.Dimension)
     */
    public synchronized void setCanvasSize(Dimension dim) {
        canvasSize = new Dimension(dim);
        if (drawMap != null) {
            setCanvasSize();
        }
    }

    private void setCanvasSize() {
        if (canvasSize.width * unitHeight > canvasSize.height * unitWidth) {
            spriteSize = canvasSize.height / unitHeight;
        } else {
            spriteSize = canvasSize.width / unitWidth;
        }
        unitOffX = 0.5f * (canvasSize.width / spriteSize - unitWidth);
        unitOffY = 0.5f * (canvasSize.height / spriteSize - unitHeight);
        RenderConfiguration.getInstance().setSpriteSize(spriteSize);
    }

    public AbstractDrawObject getRobotByID(int id) {
        return ds.getDrawObject(id);
    }

    protected void toggleFastForward() {
        fastForward = !fastForward;
    }

    private void drawHUD(Graphics2D g2) {
        if (hudScale == null) {
            return;
        } // just in case

        // update wins
        sideA.setWins(aWins, bWins);
        sideB.setWins(aWins, bWins);
        //sideA.setPointsText();

        AffineTransform pushed = g2.getTransform();
        {
            g2.transform(hudScale);
            sideA.draw(g2);
        }
        g2.setTransform(pushed);
        {
            g2.translate(/*unitWidth - */unitHUDwidth, 0);
            g2.transform(hudScale);
            sideB.setFooterText(String.format("%04d",
                    maxRounds - timeline.getRound()));
            sideB.draw(g2);
        }
        g2.setTransform(pushed);
      
        
    }

    public void setDebugState(DebugState dbg) {
        this.debugState = dbg;
    }

    public DebugState getDebugState() {
        return debugState;
    }

    private void drawState(Graphics2D g2, boolean isGraphicsStable) {
        if (drawMap == null || ds == null) {
            return;
        } // just in case
        AffineTransform pushed = g2.getTransform();
        {
            g2.translate(unitHUDwidth * 2, 0);
            drawMap.draw(g2, ds);
            g2.clip(clipRect);
            g2.translate(-origin.getX(), -origin.getY());
            if (isGraphicsStable) {
                RenderConfiguration.getInstance().updateMapTransform(g2.getTransform());
            }
            ds.draw(g2, debugState);
            g2.setClip(null);
        }
        g2.setTransform(pushed);
    }

    public void draw(Graphics g) {
        // remove if anything breaks
        if (canvasSize == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        boolean isGraphicsStable = g2.getTransform().isIdentity();
        g2.setColor(Color.BLACK);
        g2.fill(new Rectangle(0, 0, canvasSize.width, canvasSize.height));
        g2.scale(spriteSize, spriteSize);
        g2.translate(unitOffX, unitOffY);
        if (timeline.getRound() >= 0) {
            drawHUD(g2);
            drawState(g2, isGraphicsStable);
        }
        if (cutScene != null) {
            cutScene.draw(g2);
        }
        //fps.updateFramerate(); renderFramerate();
        if (fastForward) {
            timeline.setRound(timeline.getRound() + 1);
        }
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#beginIntroCutScene(long)
     */
    public void beginIntroCutScene(long targetMillis) {
        cutScene.setTargetEnd(targetMillis);
        setCutSceneVisible(true);
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#setCutSceneVisible(boolean)
     */
    public void setCutSceneVisible(boolean visible) {
        if (visible) {
            if (timeline.getRound() == -1) {
                cutScene.step = DrawCutScene.Step.INTRO;
            } else if (timeline.getRound() == timeline.getNumRounds()) {
                cutScene.step = DrawCutScene.Step.OUTRO;
            }
        }
        cutScene.setVisible(visible);
    }

    /* (non-Javadoc)
     * @see battlecode.client.viewer.render.BaseRenderer#fadeOutCutScene()
     */
    public void fadeOutCutScene() {
        cutScene.fadeOut();
    }

    private void renderFramerate(Graphics2D g2) {
        g2.setTransform(new AffineTransform());
        g2.setColor(Color.BLACK);
        g2.setFont(debugFont);
        g2.drawString("Framerate: " + fps.getFramerate(), 20, 30);
    }

    public static void preloadGraphics() {
        DrawObject.loadAll();
    }

    public Dimension getPreferredSize() {
        return new Dimension(Math.round(spriteSize * unitWidth), Math.round(spriteSize * unitHeight));
    }
}
