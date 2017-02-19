package com.cinnamon.demo;

import com.cinnamon.gfx.Canvas;
import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.object.Room;
import com.cinnamon.object.Room2D;
import com.cinnamon.system.*;

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

    private KeyEventHandler upAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX(), mPlayer.getY() + 10);
        }
    };

    private KeyEventHandler rightAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX() + 10, mPlayer.getY());
        }
    };

    private KeyEventHandler downAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX(), mPlayer.getY() - 10);
        }
    };

    private KeyEventHandler leftAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            mPlayer.moveTo(mPlayer.getX() - 10, mPlayer.getY());
        }
    };

    /**
     * <p>Show selected {@link GObject}.</p>
     */
    private MouseEventHandler showAction = new MouseEventHandler()
    {
        @Override
        public void handle(MouseEvent event)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final ImageComponent img = obj.getImageComponent();
            img.setVisible(true);
        }
    };

    /**
     * <p>Hide selected {@link GObject}.</p>
     */
    private MouseEventHandler hideAction = new MouseEventHandler()
    {
        @Override
        public void handle(MouseEvent event)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final ImageComponent img = obj.getImageComponent();
            img.setVisible(false);
        }
    };

    public DemoGame(Resources resources, Services services, Canvas canvas, Map<String, String> properties)
    {
        super(resources, services, canvas, properties);
    }

    @Override
    protected void onBegin() {
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
        final ControlMap input = getControlMap();
        input.attach(KeyEvent.Key.KEY_UP, upAction);
        input.attach(KeyEvent.Key.KEY_RIGHT, rightAction);
        input.attach(KeyEvent.Key.KEY_DOWN, downAction);
        input.attach(KeyEvent.Key.KEY_LEFT, leftAction);
        input.attach(MouseEvent.Button.RIGHT, hideAction);
        input.attach(MouseEvent.Button.MIDDLE, showAction);

        // Keep View from leaving the Room
        getView().setRoomConstrained(true);
    }

    @Override
    protected void onUpdate()
    {
        getView().moveToCenter(mPlayer);
    }

    @Override
    protected void onEnd()
    {
        getGObjectFactory().clear();
    }
}