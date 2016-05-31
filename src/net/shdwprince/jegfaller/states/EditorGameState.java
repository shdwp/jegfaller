package net.shdwprince.jegfaller.states;

import de.matthiasmann.twl.*;
import it.twl.util.BasicTWLGameState;
import it.twl.util.RootPane;
import net.shdwprince.jegfaller.JegFaller;
import net.shdwprince.jegfaller.lib.rhythm.Beatmap;
import net.shdwprince.jegfaller.lib.ui.BeatmapVisualizer;
import net.shdwprince.jegfaller.lib.ui.MusicProgressVisualizer;
import net.shdwprince.jegfaller.lib.ui.UIHelper;
import org.newdawn.slick.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.state.StateBasedGame;

import java.io.File;
import java.util.Vector;

/**
 * Created by sp on 5/27/16.
 */
public class EditorGameState extends BasicTWLGameState {
    protected StateBasedGame game;
    protected Image background;
    protected BeatmapVisualizer beatmapVisualizer;

    protected File beatmapFile;
    protected Beatmap beatmap;
    protected Music music;
    protected float musicPosition;

    protected long bmpMusicStart;
    protected long bmpLastKeyPress;
    protected Vector<Long> bpmMeterBeats;
    protected int state;

    @Override
    public int getID() {
        return JegFaller.EDITOR;
    }

    @Override
    public void init(GameContainer gameContainer, StateBasedGame stateBasedGame) throws SlickException {
        this.game = stateBasedGame;

        this.beatmapVisualizer = new BeatmapVisualizer(new Rectangle(0, 300, gameContainer.getWidth(), 200));
        this.background = new Image("assets/bg.png");
        this.background.setAlpha(0.5f);
        this.bpmMeterBeats = new Vector<>();
    }

    @Override
    public void render(GameContainer gameContainer, StateBasedGame stateBasedGame, Graphics graphics) throws SlickException {
        graphics.drawImage(this.background, 0, 0);
        graphics.setColor(Color.white);

        if (this.state == 1 && this.bmpLastKeyPress > 0) {
            long delta = (System.currentTimeMillis() - this.bmpLastKeyPress) / 3;
            float c = ((float) 255 - delta) / 255;
            graphics.setColor(new Color(c, c, c));
            graphics.fillRect(300, 300, 300, 300);
        } else if (this.state == 10) {
            this.beatmapVisualizer.render(graphics);
        }
    }

    @Override
    public void update(GameContainer gameContainer, StateBasedGame stateBasedGame, int skip) throws SlickException {
        Input i = gameContainer.getInput();
        if (this.getRootPane() != null)
            this.updateUI();

        switch (this.state) {
            case 0:
                if (i.isKeyPressed(Input.KEY_SPACE)) {
                    this.state = 1;
                    this.music.play();
                    this.bmpMusicStart = System.currentTimeMillis();
                }
                break;
            case 1:
                if (i.isKeyPressed(Input.KEY_SPACE)) {
                    this.bmpLastKeyPress = System.currentTimeMillis();
                    this.bpmMeterBeats.add(System.currentTimeMillis());
                }

                if (i.isKeyPressed(Input.KEY_ENTER)) {
                    this.calculateIntervals();
                    this.state = 10;
                }
                break;
            case 10:
                if (this.music.playing()) {
                    this.musicPosition = this.music.getPosition();
                    this.beatmapVisualizer.musicPosition = this.musicPosition;
                }

                if (i.isKeyPressed(Input.KEY_SPACE)) {
                    this.playpause();
                } else if (i.isKeyPressed(Input.KEY_A)) {
                    this.seekBackward();
                } else if (i.isKeyPressed(Input.KEY_D)) {
                    this.seekForward();
                } else if (i.isKeyPressed(Input.KEY_UP)) {
                    this.insertAction(1);
                } else if (i.isKeyPressed(Input.KEY_DOWN)) {
                    this.insertAction(2);
                } else if (i.isKeyPressed(Input.KEY_LEFT)) {
                    this.insertAction(3);
                } else if (i.isKeyPressed(Input.KEY_RIGHT)) {
                    this.insertAction(4);
                } if (i.isKeyPressed(Input.KEY_BACK)) {
                    this.removeAction();
                }

                break;
        }
    }

    @Override
    public void enter(GameContainer gameContainer, StateBasedGame stateBasedGame) throws SlickException {
        super.enter(gameContainer, stateBasedGame);
        gameContainer.getInput().enableKeyRepeat();
        this.state = -1;
        this.music = null;
        this.beatmap = null;

        if (false) {
            try {
                this.music = new Music("assets/Colors.jfb/music.ogg");
                this.music.play();
                this.music.pause();
                this.music.setVolume(0.3f);
                this.beatmap = Beatmap.beatmapBasedOn("assets/Colors.jfb/beatmap.dat");
                this.beatmapVisualizer.beatmap = this.beatmap;
                this.beatmap.totalLengthFrom(this.music);

                this.state = 0;
            } catch (Exception e) {
                e.printStackTrace();
                throw new SlickException("failed to load beatmap");
            }
        }
    }

    @Override
    public void leave(GameContainer gameContainer, StateBasedGame stateBasedGame) throws SlickException {
        super.leave(gameContainer, stateBasedGame);
        if (this.music != null) {
            this.music.pause();
            this.music = null;
        }
    }

    private Label editorStatusLabel;

    private ProgressBar playerControlProgress;
    private Button playerControlPlayPauseButton;

    private ValueAdjusterFloat bmpAdjuster, beatOffsetAdjuster, visualizerScaleAdjuster;
    private Label beatmapStatsLabel;

    private BoxLayout statusbarPane, playerControlPane, composerHelperPane, bmpHelperPane;

    @Override
    protected RootPane createRootPane() {
        RootPane p = super.createRootPane();
        p.setTheme("");

        this.editorStatusLabel = new Label("");

        this.createStatusbar();
        this.createPlayerControl();
        this.createBmpHelperPane();
        this.createComposerHelperPane();

        p.add(this.statusbarPane);
        p.add(this.playerControlPane);
        p.add(this.composerHelperPane);
        p.add(this.bmpHelperPane);

        this.setNoKeyboard(p);
        p.setCanAcceptKeyboardFocus(true);
        return p;
    }

    private void setNoKeyboard(Widget root) {
        root.setCanAcceptKeyboardFocus(false);
        for (int i = 0; i < root.getNumChildren(); i++) {
            this.setNoKeyboard(root.getChild(i));
        }
    }

    @Override
    protected void layoutRootPane() {
        super.layoutRootPane();
        this.statusbarPane.setPosition(0, 0);
        this.statusbarPane.adjustSize();

        this.playerControlPane.adjustSize();
        this.playerControlPane.setPosition(1024 - this.playerControlPane.getWidth(), 0);

        this.bmpHelperPane.setPosition(0, this.statusbarPane.getHeight());
        this.bmpHelperPane.adjustSize();
        this.composerHelperPane.setPosition(0, this.bmpHelperPane.getHeight() + this.bmpHelperPane.getY());
        this.composerHelperPane.adjustSize();
    }

    protected void createStatusbar() {
        this.statusbarPane = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        this.statusbarPane.setTheme("statusbar");

        this.statusbarPane.add(this.editorStatusLabel);

        Button b;
        b = new Button("New");
        b.addCallback(this::reset);
        this.statusbarPane.add(b);

        b = new Button("Open");
        b.addCallback(this::load);
        this.statusbarPane.add(b);

        b = new Button("Save");
        b.addCallback(this::save);
        this.statusbarPane.add(b);

        b = new Button("Quit");
        b.addCallback(() -> {
            this.game.enterState(JegFaller.MAINMENU);
        });
        this.statusbarPane.add(b);

        this.statusbarPane.setPosition(30, 30);
    }

    protected void createPlayerControl() {
        EditorGameState that = this;
        BoxLayout controls = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        Button b;

        b = new Button("<<");
        b.addCallback(this::seekBackward);
        controls.add(b);

        b = new Button("[]");
        b.addCallback(this::stop);
        controls.add(b);

        this.playerControlPlayPauseButton = new Button("l>");
        this.playerControlPlayPauseButton.addCallback(this::playpause);
        controls.add(this.playerControlPlayPauseButton);

        b = new Button(">>");
        b.addCallback(this::seekForward);
        controls.add(b);

        ProgressBar volumeBar = new ProgressBar();
        volumeBar.setValue(1.f);
        volumeBar.setText("Volume");

        b = new Button("-");
        b.addCallback(() -> {
            this.music.setVolume(this.music.getVolume()-0.1f);
            volumeBar.setValue(this.music.getVolume());
        });
        controls.add(b);
        controls.add(volumeBar);

        b = new Button("+");
        b.addCallback(() -> {
            this.music.setVolume(this.music.getVolume()+0.1f);
            volumeBar.setValue(this.music.getVolume());
        });
        b.setCanAcceptKeyboardFocus(false);
        controls.add(b);

        BoxLayout progress = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        this.playerControlProgress = new ProgressBar();
        this.playerControlProgress.setValue(0.f);
        this.playerControlProgress.setText("");
        this.playerControlProgress.setTheme("songprogress");
        progress.add(this.playerControlProgress);

        this.playerControlPane = new BoxLayout(BoxLayout.Direction.VERTICAL);
        this.playerControlPane.setTheme("statusbar");
        this.playerControlPane.add(progress);
        this.playerControlPane.add(controls);
    }

    protected void createComposerHelperPane() {
        BoxLayout scaleBox = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        this.visualizerScaleAdjuster = new ValueAdjusterFloat();
        this.visualizerScaleAdjuster.setValue(this.beatmapVisualizer.scale);
        this.visualizerScaleAdjuster.setMinMaxValue(33.f, 333.f);
        this.visualizerScaleAdjuster.setStepSize(33.f);
        scaleBox.add(new Label("Visualizer scale: "));
        scaleBox.add(this.visualizerScaleAdjuster);

        this.composerHelperPane = new BoxLayout(BoxLayout.Direction.VERTICAL);
        this.composerHelperPane.setTheme("statusbar");
        this.composerHelperPane.add(scaleBox);
    }

    protected void createBmpHelperPane() {
        BoxLayout bmpBox = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        this.bmpAdjuster = new ValueAdjusterFloat();
        this.bmpAdjuster.setMinMaxValue(0.001f, 3.333f);
        this.bmpAdjuster.setStepSize(0.005f);
        this.bmpAdjuster.setFormat("%2.3f");
        bmpBox.add(new Label("Beat size: "));
        bmpBox.add(this.bmpAdjuster);

        BoxLayout offsetBox = new BoxLayout(BoxLayout.Direction.HORIZONTAL);
        this.beatOffsetAdjuster = new ValueAdjusterFloat();
        this.beatOffsetAdjuster.setMinMaxValue(0.f, 600.f);
        this.beatOffsetAdjuster.setStepSize(0.05f);
        this.beatOffsetAdjuster.setFormat("%2.3f");
        offsetBox.add(new Label("Beat offset: "));
        offsetBox.add(this.beatOffsetAdjuster);

        this.beatmapStatsLabel = new Label("");

        this.bmpHelperPane = new BoxLayout(BoxLayout.Direction.VERTICAL);
        this.bmpHelperPane.setTheme("pane");
        this.bmpHelperPane.add(bmpBox);
        this.bmpHelperPane.add(offsetBox);
        this.bmpHelperPane.add(this.beatmapStatsLabel);
    }

    protected void updateUI() {
        this.composerHelperPane.adjustSize();
        this.bmpHelperPane.adjustSize();

        if (this.beatmap != null) {
            this.beatmapStatsLabel.setText(String.format("Total: %d actions", this.beatmap.actions == null ? 0 : this.beatmap.actions.length));

            if (this.bmpAdjuster.getValue() == 0.001f) {
                this.bmpAdjuster.setValue(this.beatmap.beatSize);
            } else if (this.bmpAdjuster.getValue() != this.beatmap.beatSize) {
                this.beatmap.beatSize = this.bmpAdjuster.getValue();
                this.beatmap.createActionsArray();
            }

            if (this.beatOffsetAdjuster.getValue() == 0.f) {
                this.beatOffsetAdjuster.setValue(this.beatmap.beatOffset);
            } else if (this.beatOffsetAdjuster.getValue() != this.beatmap.beatOffset) {
                this.beatmap.beatOffset = this.beatOffsetAdjuster.getValue();
                this.beatmap.createActionsArray();
            }

            this.playerControlProgress.setValue(this.musicPosition / this.beatmap.totalLength);
            this.playerControlProgress.setText(String.format("%.2f / %.2f", this.musicPosition, this.beatmap.totalLength));
        }

        this.beatmapVisualizer.scale = this.visualizerScaleAdjuster.getValue();

        if (this.music != null) {
            if (this.music.playing()) {
                this.playerControlPlayPauseButton.setText("||");
            } else {
                this.playerControlPlayPauseButton.setText("l>");
            }
        }

        switch (this.state) {
            case -1:
                this.editorStatusLabel.setText("Waiting");
                break;
            case 0:
                this.editorStatusLabel.setText("Music loaded");
                break;
            case 1:
                this.editorStatusLabel.setText("Determining beat size and offset");
                break;
            case 10:
                this.editorStatusLabel.setText("Composing");
                break;
        }
    }

    protected void resetUI() {
        this.beatOffsetAdjuster.setValue(0.f);
        this.bmpAdjuster.setValue(0.001f);
    }

    protected void reset() {
        File path;
        if ((path = UIHelper.instance().selectFile()) != null) {
            try {
                this.bpmMeterBeats = new Vector<>();
                this.bmpLastKeyPress = 0;
                this.bmpMusicStart = 0;

                this.music = new Music(path.getParent() + File.separator + "music.ogg");
                this.beatmap = new Beatmap();
                this.beatmap.totalLengthFrom(this.music);
                this.beatmapVisualizer.beatmap = this.beatmap;

                this.state = 0;
                this.music.stop();

                this.resetUI();
            } catch (Exception e) {
                e.printStackTrace();
                // @TODO: alert exception
            }
        }
    }

    protected void load() {
        File path;
        if ((path = UIHelper.instance().selectFile()) != null) {
            try {
                this.beatmap = Beatmap.beatmapBasedOn(path.getAbsolutePath());
                this.beatmapVisualizer.beatmap = this.beatmap;

                this.state = 10;
                this.music = new Music(path.getParent() + File.separator + "music.ogg");
                this.music.play();
                this.music.pause();
                this.resetUI();
            } catch (Exception e) {
                e.printStackTrace();
                // @TODO: alert error
            }
        }
    }

    protected void save() {
        if (this.beatmapFile != null) {
            try {
                this.beatmap.saveIn(this.beatmapFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                // @TODO: alert exception
            }
        } else {
            File path;
            if ((path = UIHelper.instance().selectFile()) != null) {
                this.beatmapFile = new File(path + File.separator + "beatmap.dat");
                this.save();
            }
        }
    }

    protected void calculateIntervals2() {
        long count = 0;
        long intervalTotal = 0;
        long previous = 0;
        long offset = 0;
        for (Long beatTime : this.bpmMeterBeats) {
            if (previous == 0) {
                offset = beatTime - this.bmpMusicStart;
            } else {
                count++;
                intervalTotal += beatTime - previous;
            }

            previous = beatTime;
        }

        this.beatmap.beatSize = (float) (intervalTotal/count) / 1000;
        this.beatmap.beatOffset = (float) offset / 1000;

        this.beatmap.createActionsArray();
    }

    protected void calculateIntervals() {
        long offset = this.bpmMeterBeats.firstElement() - this.bmpMusicStart;
        Vector<Long> diffs = new Vector<>();
        int checkedBeatsLength = 4;

        int minDiffIdx = -1;
        float minDiff = 100;

        for (int i = 1; i < this.bpmMeterBeats.size(); i++) {
            float beatMaxDiff = 0;
            for (int check = 1; check < checkedBeatsLength; check++) {
                float diff = 0;
                try {
                    diff = Math.abs(this.bpmMeterBeatSizeAt(i) - this.bpmMeterBeatSizeAt(i + check));
                } catch (ArrayIndexOutOfBoundsException ex) {
                    diff = 100;
                }

                if (diff > beatMaxDiff) {
                    beatMaxDiff = diff;
                }
            }

            if (minDiff > beatMaxDiff) {
                minDiff = beatMaxDiff;
                minDiffIdx = i;
            }
        }

        this.beatmap.beatSize = this.bpmMeterBeatSizeAt(minDiffIdx) / 1000;
        this.beatmap.beatOffset = (float) offset / 1000;
        this.beatmap.createActionsArray();
    }

    protected float bpmMeterBeatSizeAt(int i) {
        return (float) (this.bpmMeterBeats.elementAt(i) - this.bpmMeterBeats.elementAt(i-1));
    }

    protected void insertAction(int action) {
        int idx = this.beatmap.closestHitFrom(this.musicPosition).idx;
        this.beatmap.actions[idx] = action;
    }

    protected void removeAction() {
        int idx = this.beatmap.closestHitFrom(this.musicPosition).idx;
        this.beatmap.actions[idx] = 0;
    }

    protected void stop() {
        this.musicPosition = 0.f;
        this.music.setPosition(0.f);
        this.music.pause();
    }

    protected void playpause() {
        if (this.music.playing()) {
            this.music.pause();
        } else {
            this.music.resume();
            this.music.setPosition(this.musicPosition);

        }
    }

    protected void seekBackward() {
        float prev = this.beatmap.prevOffsetFrom(this.musicPosition-0.001f);
        this.musicPosition = prev;
        this.beatmapVisualizer.musicPosition = this.musicPosition;
        this.music.setPosition(prev);
    }

    protected void seekForward() {
        float next = this.beatmap.nextOffsetFrom(this.musicPosition+0.001f);
        this.musicPosition = next;
        this.beatmapVisualizer.musicPosition = this.musicPosition;
        this.music.setPosition(next);
    }
}
