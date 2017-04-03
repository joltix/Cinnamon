package com.cinnamon.gfx;

import com.cinnamon.system.View;

/**
 * <p>
 *     Container for a "snapshot" of a moment in the game. Scenes are to be filled with enough drawing information to
 *     transfer from the game's main update thread to the drawing thread.
 * </p>
 *
 * <p>
 *     The drawing data should not be references to data that may be modified at any moment from the game's update
 *     thread as the data will be accessed by the drawing thread. Lightweight copies are recommended.
 * </p>
 *
 * @param <E> object delivering drawing data.
 */
public interface Scene<E>
{
    /**
     * <p>Gets the next {@link E} from the Scene for drawing.</p>
     *
     * @return drawing data.
     */
    E poll();

    /**
     * <p>Adds all necessary drawing information to the Scene.</p>
     *
     * @param factory ImageFactory storing drawing data.
     * @param view View for filtering for visible instances.
     */
    void add(ImageFactory factory, View view);

    /**
     * <p>Empties the Scene of all drawing data.</p>
     */
    void clear();

    /**
     * <p>Makes available all previously polled drawing data and moves the polling cursor back to the beginning. The
     * Scene is reset to the state before {@link #poll()} was called. This method will have no effect if
     * {@link #clear()} has already been called.</p>
     */
    void reset();

    /**
     * <p>Gets the number of drawable instances.</p>
     *
     * @return instance count.
     */
    int size();

    /**
     * <p>Checks whether or not the Scene has any drawing information left.</p>
     *
     * @return true if the {@link #size()} is 0.
     */
    boolean isEmpty();
}
