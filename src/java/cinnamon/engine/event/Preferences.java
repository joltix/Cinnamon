package cinnamon.engine.event;

/**
 * <p>This interface only specifies a priority attribute intended to be sorted on and is meant for classes carrying
 * specifying information for reacting to input.</p>
 */
interface Preferences
{
    /**
     * <p>Gets the priority.</p>
     *
     * @return priority.
     */
    int getPriority();
}
