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
 */
public class DemoGame extends Game
{
    private KeyEventHandler mCloseAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent event)
        {
            DemoGame.this.stop();
        }
    };

    private KeyEventHandler mUpAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj != null) {
                obj.moveTo(obj.getX(), obj.getY() + 10);
            }
        }
    };

    private KeyEventHandler mRightAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj != null) {
                obj.moveTo(obj.getX() + 10, obj.getY());
            }
        }
    };

    private KeyEventHandler mDownAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj != null) {
                obj.moveTo(obj.getX(), obj.getY() - 10);
            }
        }
    };

    private KeyEventHandler mLeftAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj != null) {
                obj.moveTo(obj.getX() - 10, obj.getY());
            }
        }
    };

    /**
     * <p>Show selected {@link GObject}.</p>
     */
    private MouseEventHandler mShowAction = new MouseEventHandler()
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
    private MouseEventHandler mHideAction = new MouseEventHandler()
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
        final GObject obj = goFactory.getGObject("char");
        obj.moveTo(300, 300);
        obj.getImageComponent().setTint(0.8f, 1f, 1f);
        this.setSelected(obj);

        // Create untextured character
        final GObject character = goFactory.getGObject("char");
        character.moveTo(200, 200);
        character.getImageComponent().setTexture(Texture.NULL);
        character.getImageComponent().setHeight(100f);
        character.getImageComponent().setWidth(100f);
        character.getImageComponent().setTint(0.1f, 0.8f, 0.1f);

        // Hook View position into arrow keys
        final ControlMap input = getControlMap();
        input.attach(KeyEvent.Key.KEY_UP, mUpAction);
        input.attach(KeyEvent.Key.KEY_RIGHT, mRightAction);
        input.attach(KeyEvent.Key.KEY_DOWN, mDownAction);
        input.attach(KeyEvent.Key.KEY_LEFT, mLeftAction);

        // Allow game shutdown from ESC key
        input.attach(KeyEvent.Key.KEY_ESCAPE, mCloseAction);

        // Hide and show selected object
        input.attach(MouseEvent.Button.RIGHT, mHideAction);
        input.attach(MouseEvent.Button.MIDDLE, mShowAction);

        // Keep View from leaving the Room
        getView().setRoomConstrained(true);
    }

    @Override
    protected void onUpdate()
    {
        final GObject obj = getSelected();
        if (obj != null) {
            getView().moveToCenter(obj);
        }
    }

    @Override
    protected void onEnd()
    {
        getGObjectFactory().clear();
        getBodyFactory().clear();
        getImageFactory().clear();
    }
}