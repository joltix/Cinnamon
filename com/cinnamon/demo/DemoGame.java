package com.cinnamon.demo;

import com.cinnamon.gfx.Canvas;
import com.cinnamon.gfx.Texture;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.object.Room;
import com.cinnamon.object.Room2D;
import com.cinnamon.system.ControlMap;
import com.cinnamon.system.Game;
import com.cinnamon.system.InputMap;
import com.cinnamon.utils.KeyEvent;

import java.util.Map;

/**
 * <p>
 *     Demo {@link Game}.
 * </p>
 *
 *
 */
public class DemoGame extends Game
{
    private GObject mPlayer;

    private ControlMap.Action upAction = new ControlMap.Action<KeyEvent>()
    {
        @Override
        public void execute(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX(), mPlayer.getY() + 10);
        }
    };

    private ControlMap.Action rightAction = new ControlMap.Action<KeyEvent>()
    {
        @Override
        public void execute(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX() + 10, mPlayer.getY());
        }
    };

    private ControlMap.Action downAction = new ControlMap.Action<KeyEvent>()
    {
        @Override
        public void execute(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX(), mPlayer.getY() - 10);
        }
    };

    private ControlMap.Action leftAction = new ControlMap.Action<KeyEvent>()
    {
        @Override
        public void execute(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX() - 10, mPlayer.getY());
        }
    };

    public DemoGame(Resources services, Canvas canvas, Map<String,
            String> properties)
    {
        super(services, canvas, properties);
        setTickrate(60);

    }

    @Override
    protected void onBegin() {
        System.out.printf(DemoGame.class + "::onBegin()\n");
        final GObjectFactory goFactory = this.getGObjectFactory();

        // Create Room
        final Room room = new Room2D(goFactory, 3840,2160);
        final int backgroundId = getShaderFactory().getTexture
                ("demo_background.png").getId();
        room.setBackgroundImage(backgroundId);
        this.setRoom(room);

        // Create player
        mPlayer = goFactory.getGObject("character");
        mPlayer.moveTo(300, 300);
        mPlayer.getImageComponent().setTint(0.8f, 1f, 1f);

        // Create untextured character
        final GObject character = goFactory.getGObject("character");
        character.moveTo(200, 200);
        character.getImageComponent().setTexture(Texture.NULL);
        character.getImageComponent().setHeight(100f);
        character.getImageComponent().setWidth(100f);
        character.getImageComponent().setTint(0.1f, 0.8f, 0.1f);

        // Hook View position into arrow keys
        final InputMap input = getInput();
        input.attachKey(KeyEvent.KEY_UP, upAction);
        input.attachKey(KeyEvent.KEY_RIGHT, rightAction);
        input.attachKey(KeyEvent.KEY_DOWN, downAction);
        input.attachKey(KeyEvent.KEY_LEFT, leftAction);

        // Keep View from leaving the Room
        getView().setRoomConstrained(true);
    }

    @Override
    protected void update()
    {
        getView().moveToCenter(mPlayer);
    }

    @Override
    protected void onEnd()
    {
        getGObjectFactory().clear();
    }
}